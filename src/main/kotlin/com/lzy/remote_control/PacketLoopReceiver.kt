package com.lzy.remote_control

import com.lzy.remote_control.network.*
import com.lzy.remote_control.protocol.NetworkPacket
import com.lzy.remote_control.protocol.ubytesToInt
import javafx.application.Platform

class PacketLoopReceiver(_sslThread: SSLThread, _callback: PacketReceiveHandler) : SSLThreadTransferCallback {
    private var recvStep = 1
    private var bytesRemain = LENGTH_BEFORE_CONTENT
    private var contentLength = 0

    private val sslThread = _sslThread

    private val callback = _callback

    private val beforeContentBuffer = ByteArray(LENGTH_BEFORE_CONTENT)
    private val afterContentBuffer = ByteArray(LENGTH_AFTER_CONTENT)
    private var contentBytes: ByteArray? = null

    companion object {
        private const val RECEIVE_PACKET_BEGIN_TYPE_AND_LENGTH = 1
        private const val RECEIVE_PACKET_CONTENT = 2
        private const val RECEIVE_PACKET_CRC_AND_PACKET_END = 3

        private const val LENGTH_BEFORE_CONTENT = 11
        private const val LENGTH_AFTER_CONTENT = 7

        private const val CONTENT_LENGTH_OFFSET = 7
    }

    fun postReceiveBytes() {
        val messageParam = SSLThreadTransferParam(ByteArray(0), 0, 0, true, this)

        synchronized(this) {
            if (bytesRemain == 0) {
                ++recvStep

                if (recvStep > RECEIVE_PACKET_CRC_AND_PACKET_END)
                    recvStep = RECEIVE_PACKET_BEGIN_TYPE_AND_LENGTH

                if (recvStep == RECEIVE_PACKET_CONTENT) {
                    contentLength = ubytesToInt(beforeContentBuffer.map { it.toUByte() }.toTypedArray(), CONTENT_LENGTH_OFFSET)
                    contentBytes = ByteArray(contentLength)
                }
            }

            when (recvStep) {
                RECEIVE_PACKET_BEGIN_TYPE_AND_LENGTH -> {
                    messageParam.buffer = beforeContentBuffer
                    messageParam.offset = beforeContentBuffer.size - bytesRemain
                    messageParam.length = bytesRemain
                }
                RECEIVE_PACKET_CONTENT -> {
                    messageParam.buffer = contentBytes!!
                    messageParam.offset = contentLength - bytesRemain
                    messageParam.length = bytesRemain
                }
                RECEIVE_PACKET_CRC_AND_PACKET_END -> {
                    messageParam.buffer = afterContentBuffer
                    messageParam.offset = afterContentBuffer.size - bytesRemain
                    messageParam.length = bytesRemain
                }
            }
        }

        sslThread.startTransfer(messageParam)
    }

    override fun onTransferCompleted(
        bytes: ByteArray,
        offset: Int,
        length: Int,
        transferBytes: Int,
        isReceive: Boolean,
        exception: Exception?
    )
    {
        if (exception == null) {
            synchronized(this) {
                if (recvStep == RECEIVE_PACKET_CRC_AND_PACKET_END && bytesRemain - length == 0) {
                    val ubyteArray =
                        Array<UByte>(beforeContentBuffer.size + contentBytes!!.size + afterContentBuffer.size) { index ->
                            if (index < beforeContentBuffer.size)
                                beforeContentBuffer[index].toUByte()
                            else if (index - LENGTH_BEFORE_CONTENT < contentBytes!!.size)
                                contentBytes!![index - LENGTH_BEFORE_CONTENT].toUByte()
                            else if (index - LENGTH_BEFORE_CONTENT - contentBytes!!.size < afterContentBuffer.size)
                                afterContentBuffer[index - LENGTH_BEFORE_CONTENT - contentBytes!!.size].toUByte()
                            else
                                0.toUByte()
                        }
                    try {
                        val packet = NetworkPacket()
                        packet.fromUBytes(ubyteArray)

                        Platform.runLater {
                            callback.onPacketReceived(packet, sslThread)
                        }

                    } catch (exception: Exception) {
                        Platform.runLater {
                            callback.onPacketReceiveError(exception, sslThread)
                        }
                    }
                }
            }
            postReceiveBytes()
        }
        else {
            Platform.runLater {
                callback.onPacketReceiveError(exception, sslThread)
            }
        }
    }
}