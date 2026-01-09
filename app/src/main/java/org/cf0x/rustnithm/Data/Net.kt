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

    private val sendBuffer = ByteArray(48)
    private val packet = DatagramPacket(sendBuffer, 48)

    fun start(ip: String, port: Int) {
        scope.launch {
            try {
                if (socket == null) {
                    socket = DatagramSocket()
                }
                address = InetAddress.getByName(ip)
                targetPort = port
                packet.address = address
                packet.port = targetPort
                for (i in 0..7) sendBuffer[i] = 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendFullState(
        air: Set<Int>,
        slide: Set<Int>,
        coin: Boolean,
        service: Boolean,
        test: Boolean
    ) {
        for (i in 8..47) sendBuffer[i] = 0
        air.forEach { id ->
            val idx = 7 + id
            if (idx in 8..13) sendBuffer[idx] = 1
        }
        slide.forEach { id ->
            val idx = 13 + id


            if (idx in 14..45) {
                sendBuffer[idx] = 1
            }
        }
        sendBuffer[46] = if (coin) 1 else 0
        var flags = 0
        if (service) flags = flags or 0x01
        if (test) flags = flags or 0x02
        sendBuffer[47] = flags.toByte()
        scope.launch {
            try {
                socket?.send(packet)
            } catch (e: Exception) {
            }
        }
    }

    fun close() {
        socket?.close()
        socket = null
        address = null
    }
}