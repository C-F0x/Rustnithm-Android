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
    private val sendBuffer = ByteArray(11)
    private var packet: DatagramPacket? = null

    fun start(ip: String, port: Int) {
        scope.launch {
            try {
                stop()
                socket = DatagramSocket().apply { sendBufferSize = 64 * 1024 }
                address = InetAddress.getByName(ip)
                targetPort = port
                packet = DatagramPacket(sendBuffer, 11, address, targetPort)
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
        test: Boolean,
        isCardActive: Boolean,
        accessCode: String
    ) {
        val currentSocket = socket ?: return
        val currentPacket = packet ?: return

        scope.launch {
            try {
                if (isCardActive && accessCode.length == 20) {
                    sendBuffer[0] = createHeader(false, false, 0b11)
                    for (i in 0 until 10) {
                        val high = accessCode[i * 2].digitToInt()
                        val low = accessCode[i * 2 + 1].digitToInt()
                        sendBuffer[i + 1] = ((high shl 4) or low).toByte()
                    }
                    currentPacket.length = 11
                }
                else if (coin || service || test) {
                    sendBuffer[0] = createHeader(false, false, 0b01)
                    var mask = 0
                    if (coin) mask = mask or 0x01
                    if (service) mask = mask or 0x02
                    if (test) mask = mask or 0x04
                    sendBuffer[1] = mask.toByte()
                    currentPacket.length = 2
                }
                else {
                    sendBuffer[0] = createHeader(false, false, 0b10)
                    var airByte = 0
                    for (id in air) {
                        val bitIndex = id - 1
                        if (bitIndex in 0..5) {
                            airByte = airByte or (1 shl bitIndex)
                        }
                    }
                    sendBuffer[1] = airByte.toByte()

                    for (i in 2..5) sendBuffer[i] = 0
                    for (id in slide) {
                        val adjustedId = id - 1
                        if (adjustedId in 0..31) {
                            val byteIdx = 2 + (adjustedId / 8)
                            val bitIdx = 7 - (adjustedId % 8)
                            sendBuffer[byteIdx] = (sendBuffer[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                        }
                    }
                    sendBuffer[6] = 0
                    currentPacket.length = 7
                }

                currentSocket.send(currentPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createHeader(isTcp: Boolean, isServer: Boolean, type: Int): Byte {
        val tcpBit = if (isTcp) 1 else 0
        val serverBit = if (isServer) 1 else 0
        return ((tcpBit shl 7) or (serverBit shl 6) or (type shl 4)).toByte()
    }
}