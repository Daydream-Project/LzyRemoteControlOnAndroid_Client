package com.lzy.remote_control.network

import java.net.SocketTimeoutException
import java.security.KeyStore
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.TimeoutException
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

class SSLThread(private val ipPort: IPPort, private val eventCallback: SSLThreadEventCallback): Thread() {
    private var terminateFlag = false
    private val transferOperations: LinkedList<SSLThreadTransferParam> = LinkedList<SSLThreadTransferParam>()

    val IPPort: IPPort
        get() = ipPort

    fun terminate() {
        synchronized(this) {
            terminateFlag = true
        }
        if (currentThread().id == id)
            return
        while (state != State.TERMINATED) continue
    }

    fun startTransfer(transferParam: SSLThreadTransferParam) {
        synchronized(this) {
            transferOperations.offerLast(transferParam)
        }
    }

    override fun run() {
        val socket: SSLSocket
        val tmf: TrustManagerFactory
        try {
            //Create keystore key source is server.keystore.
            val ks = KeyStore.getInstance("PKCS12")
            val keyFile = javaClass.getResourceAsStream("/server.keystore")
            ks.load(keyFile, "@2003LIUzhiYING".toCharArray())

            //Init trust manager factory
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(ks)

            eventCallback.onEventOccurred(this, SSLThreadEvent.LOAD_KEYSTORE, null)
        } catch (exception: Exception) {
            eventCallback.onEventOccurred(this, SSLThreadEvent.LOAD_KEYSTORE, exception)
            return
        }

        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, tmf.trustManagers, null)

            socket = sslContext.socketFactory.createSocket(ipPort.ip, ipPort.port) as SSLSocket
            socket.soTimeout = 10000
            socket.startHandshake()
            socket.soTimeout = 1

            eventCallback.onEventOccurred(this,SSLThreadEvent.INIT_SOCKET, null)
        } catch (exception: Exception) {
            eventCallback.onEventOccurred(this,SSLThreadEvent.INIT_SOCKET, exception)
            return
        }

        var terminateFlagCopy = false

        while (!terminateFlagCopy) {
            var transferParam: SSLThreadTransferParam? = null
            synchronized(this) {
                terminateFlagCopy = terminateFlag

                if (!transferOperations.isEmpty()) {
                    transferParam = transferOperations.pollFirst()
                }
            }

            if (transferParam != null) {
                try {
                    if (transferParam!!.isReceive) {
                        val transferBytes = socket.inputStream.read(transferParam!!.buffer, transferParam!!.length, transferParam!!.offset)
                        transferParam!!.callback.onTransferCompleted(transferParam!!.buffer, transferParam!!.length, transferParam!!.offset, transferBytes, true, null)
                    } else {
                        socket.outputStream.write(transferParam!!.buffer, transferParam!!.length, transferParam!!.offset)
                        transferParam!!.callback.onTransferCompleted(transferParam!!.buffer, transferParam!!.length, transferParam!!.offset, transferParam!!.length, false, null)
                    }
                } catch (exception: SocketTimeoutException) {
                    if (transferParam!!.isReceive)
                        transferParam!!.callback.onTransferCompleted(transferParam!!.buffer, transferParam!!.length, transferParam!!.offset, 0, true, exception)
                    else
                        transferParam!!.callback.onTransferCompleted(transferParam!!.buffer, transferParam!!.length, transferParam!!.offset, 0, false, exception)
                } catch (exception: Exception) {
                    if (transferParam!!.isReceive)
                        transferParam!!.callback.onTransferCompleted(transferParam!!.buffer, transferParam!!.length, transferParam!!.offset, 0, true, exception)
                    else
                        transferParam!!.callback.onTransferCompleted(transferParam!!.buffer, transferParam!!.length, transferParam!!.offset, 0, false, exception)

                    eventCallback.onEventOccurred(this, SSLThreadEvent.CONNECT_LOST, exception)

                    break
                }
            }

            sleep(1)
        }

        socket.close()
    }
}