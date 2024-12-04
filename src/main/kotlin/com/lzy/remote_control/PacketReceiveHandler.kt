package com.lzy.remote_control

import com.lzy.remote_control.protocol.NetworkPacket
import com.lzy.remote_control.network.SSLThread
import kotlin.Exception

interface PacketReceiveHandler {
    fun onPacketReceived(packet: NetworkPacket, sslThread: SSLThread)
    fun onPacketReceiveError(exception: Exception, sslThread: SSLThread)
}