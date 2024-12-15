package com.lzy.remote_control.network

import com.lzy.remote_control.protocol.BROADCAST_INFO_PORT
import com.lzy.remote_control.protocol.BroadcastRemoteControlServer
import com.lzy.remote_control.protocol.NetworkPackage
import javafx.application.Platform
import javafx.scene.control.ListView
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class FindDeviceThread(private val callback: FindDeviceCallback): Thread() {
    private var terminateFlag = false

    fun terminate() {
        synchronized(this) {
            terminateFlag = true
        }
        if (Thread.currentThread().id == id)
            return
        while (state != Thread.State.TERMINATED) continue
    }

    override fun run() {
        super.run()

        //Create channel for receive message server broadcast.

        val channel = DatagramChannel.open()

        channel.bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), BROADCAST_INFO_PORT))
        channel.configureBlocking(false)

        val deviceList: MutableList<String> = mutableListOf()
        val protocolPackage = NetworkPackage()

        val receiveBuffer = ByteBuffer.allocate(1024)

        var terminateFlagCopy = false

        //If terminateFlag is not set, loop to receive message.

        while (!terminateFlagCopy) {
            channel.receive(receiveBuffer)

            //If a message received, handle it

            if (receiveBuffer.remaining() < receiveBuffer.capacity()) {

                val dataLength = receiveBuffer.capacity() - receiveBuffer.remaining()
                val data = Array<UByte>(dataLength, { index -> receiveBuffer[index].toUByte() })

                //try parse message.
                try {
                    protocolPackage.fromUBytes(data, 0, dataLength)
                } catch (_: Exception) {
                }

                //If the message parsed success, check the ip is valid, then do operation.
                if (protocolPackage.content is BroadcastRemoteControlServer) {
                    val info = protocolPackage.content as BroadcastRemoteControlServer
                    val ipCopy = info.ip
                    val portCopy = info.port

                    var ipIsValid = false

                    //Send udp data to check ip
                    try {
                        channel.send(receiveBuffer, InetSocketAddress(InetAddress.getByName(ipCopy), BROADCAST_INFO_PORT))
                        ipIsValid = true
                    } catch (_: Exception) {
                    }

                    //If ip is new and valid, add it in list and ui.
                    if (ipIsValid && deviceList.find { it == info.ip } == null) {
                        deviceList.add(ipCopy)

                        callback.onDeviceFound(ipCopy, portCopy)
                    }
                }

                receiveBuffer.position(0)
            } else {
                sleep(1)
            }

            //read terminate flag.
            synchronized(this) {
                terminateFlagCopy = terminateFlag
            }
        }

        //Free resource.
        channel.close()
    }
}