package org.cf0x.rustnithm.Data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

object Net {
    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null
    private var targetPort: Int = 0

    private val netDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + netDispatcher)

    private val sendBuffer = ByteArray(7)
    private val packet = DatagramPacket(sendBuffer, 7)

    fun start(ip: String, port: Int) {
        scope.launch {
            try {
                stop()
                socket = DatagramSocket().apply { sendBufferSize = 64 * 1024 }
                address = InetAddress.getByName(ip)
                targetPort = port
                packet.address = address
                packet.port = targetPort
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendFullState(
        air: Set<Int>,
        slide: Set<Int>,
        coin: Boolean,
        service: Boolean,
        test: Boolean
    ) {
        val currentSocket = socket ?: return
        scope.launch {
            try {
                // Byte 0: Header
                sendBuffer[0] = 0xA0.toByte()

                // Byte 1: Air (保持原始逻辑)
                var airByte = 0
                for (id in 1..6) {
                    if (air.contains(id)) airByte = airByte or (1 shl (8 - id))
                }
                sendBuffer[1] = airByte.toByte()

                // Byte 2-5: Slider (对齐修正：id-1)
                for (i in 2..5) sendBuffer[i] = 0

                slide.forEach { id ->
                    // 执行 id - 1 偏移校准
                    val adjustedId = id - 1
                    if (adjustedId in 0..31) {
                        val byteIdx = 2 + (adjustedId / 8)
                        val bitIdx = 7 - (adjustedId % 8)

                        val mask = (1 shl bitIdx)
                        val current = sendBuffer[byteIdx].toInt() and 0xFF
                        sendBuffer[byteIdx] = (current or mask).toByte()
                    }
                }

                // Byte 6: Check
                sendBuffer[6] = 0

                currentSocket.send(packet)
            } catch (e: Exception) {}
        }
    }
}