use jni::JNIEnv;
use jni::objects::{JClass, JString, JByteArray};
use jni::sys::{jint, jbyteArray};
use std::net::UdpSocket;
use std::sync::atomic::{AtomicBool, AtomicU32, AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::{Duration, Instant};
use once_cell::sync::Lazy;

struct NetState {
    packet_type: AtomicU32,
    button_mask: AtomicU32,
    air_byte: AtomicU32,
    slider_mask: AtomicU32,
    card_bcd: Mutex<[u8; 10]>,
}

static STATE: Lazy<Arc<NetState>> = Lazy::new(|| Arc::new(NetState {
    packet_type: AtomicU32::new(0),
    button_mask: AtomicU32::new(0),
    air_byte: AtomicU32::new(0),
    slider_mask: AtomicU32::new(0),
    card_bcd: Mutex::new([0u8; 10]),
}));

static IS_RUNNING: AtomicBool = AtomicBool::new(false);
static INTERVAL_NS: AtomicU64 = AtomicU64::new(2_000_000);
static PENDING_TASKS: AtomicU32 = AtomicU32::new(0);
const MAX_PENDING: u32 = 10;

fn create_header(p_type: u8) -> u8 {
    (p_type << 4) & 0b00110000 
}
fn run_send_loop(addr: String) {
    let socket = match UdpSocket::bind("0.0.0.0:0") {
        Ok(s) => {
            s.set_nonblocking(true).unwrap();
            s
        },
        Err(_) => return,
    };
    #[cfg(target_os = "android")]
    unsafe {
        let tid = libc::gettid();
        let param = libc::sched_param { sched_priority: 99 };
        libc::sched_setscheduler(tid, libc::SCHED_FIFO, &param);
    }

    let state = &*STATE;
    let mut last_tick = Instant::now();

    while IS_RUNNING.load(Ordering::Acquire) {
        let interval = Duration::from_nanos(INTERVAL_NS.load(Ordering::Acquire));
        
        while last_tick.elapsed() < interval {
            std::hint::spin_loop(); 
        }
        last_tick = Instant::now();

        if PENDING_TASKS.load(Ordering::Acquire) >= MAX_PENDING {
            continue;
        }

        let mut buffer = [0u8; 11];
        let mut packet_len = 0;
        let p_type = state.packet_type.load(Ordering::Relaxed);

        match p_type {
            1 => {
                buffer[0] = create_header(0b01);
                buffer[1] = state.button_mask.load(Ordering::Relaxed) as u8;
                packet_len = 2;
            },
            2 => {
                buffer[0] = create_header(0b10);
                buffer[1] = state.air_byte.load(Ordering::Relaxed) as u8;
                let s_mask = state.slider_mask.load(Ordering::Relaxed);
                buffer[2..6].copy_from_slice(&s_mask.to_be_bytes());
                buffer[6] = 0;
                packet_len = 7;
            },
            3 => {
                buffer[0] = create_header(0b11);
                if let Ok(guard) = state.card_bcd.lock() {
                    buffer[1..11].copy_from_slice(&*guard);
                }
                packet_len = 11;
            },
            _ => continue,
        }

        if packet_len > 0 {
            PENDING_TASKS.fetch_add(1, Ordering::SeqCst);
            match socket.send_to(&buffer[..packet_len], &addr) {
                Ok(_) => {
                    PENDING_TASKS.fetch_sub(1, Ordering::SeqCst);
                },
                Err(ref e) if e.kind() == std::io::ErrorKind::WouldBlock => {
                    PENDING_TASKS.fetch_sub(1, Ordering::SeqCst);
                },
                Err(_) => {
                    PENDING_TASKS.fetch_sub(1, Ordering::SeqCst);
                }
            }
        }
    }
}


#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeUpdateState(
    env: JNIEnv,
    _class: JClass,
    packet_type: jint,
    button_mask: jint,
    air_byte: jint,
    slider_mask: jint,
    card_bcd: jbyteArray,
) {
    let state = &*STATE;
    state.packet_type.store(packet_type as u32, Ordering::Relaxed);
    state.button_mask.store(button_mask as u32, Ordering::Relaxed);
    state.air_byte.store(air_byte as u32, Ordering::Relaxed);
    state.slider_mask.store(slider_mask as u32, Ordering::Relaxed);

    if packet_type == 3 && !card_bcd.is_null() {
        let obj = unsafe { JByteArray::from_raw(card_bcd) };
        if let Ok(bytes) = env.convert_byte_array(&obj) {
            if bytes.len() == 10 {
                if let Ok(mut guard) = state.card_bcd.lock() {
                    guard.copy_from_slice(&bytes);
                }
            }
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeStart(
    mut env: JNIEnv,
    _class: JClass,
    ip: JString,
    port: jint,
    frequency_hz: jint,
) {
    if IS_RUNNING.load(Ordering::Acquire) { return; }

    let ip_str: String = env.get_string(&ip).expect("Invalid IP").into();
    let addr = format!("{}:{}", ip_str, port);
    
    let hz = if frequency_hz <= 0 { 500 } else { frequency_hz as u64 };
    let interval = 1_000_000_000u64 / hz;
    INTERVAL_NS.store(interval, Ordering::SeqCst);
    IS_RUNNING.store(true, Ordering::SeqCst);

    thread::Builder::new()
        .name("RustNetEngine".to_string())
        .spawn(move || {
            run_send_loop(addr);
        })
        .expect("Failed to spawn thread");
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeSetFrequency(
    _env: JNIEnv,
    _class: JClass,
    frequency_hz: jint,
) {
    if frequency_hz > 0 {
        let interval = 1_000_000_000u64 / (frequency_hz as u64);
        INTERVAL_NS.store(interval, Ordering::SeqCst);
    }
}

#[no_mangle]
pub extern "system" fn Java_org_cf0x_rustnithm_Data_Net_nativeStop(
    _env: JNIEnv,
    _class: JClass,
) {
    IS_RUNNING.store(false, Ordering::Release);
}