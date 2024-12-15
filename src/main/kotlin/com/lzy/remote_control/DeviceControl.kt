package com.lzy.remote_control

import com.lzy.remote_control.network.IPPort
import com.lzy.remote_control.network.SSLThread
import com.lzy.remote_control.network.SSLThreadEvent
import com.lzy.remote_control.network.SSLThreadEventCallback
import com.lzy.remote_control.protocol.NetworkPacket
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage

class DeviceControl(ipPort: IPPort): PacketReceiveHandler, SSLThreadEventCallback {
    @FXML
    private lateinit var rootContainer: AnchorPane
    private val sslThread = SSLThread(ipPort, this)
    private val packetLoopReceiver = PacketLoopReceiver(sslThread, this)

    init {
        sslThread.start()
        packetLoopReceiver.postReceiveOperation()
    }

    private fun onConnectionLost() {
        val alertBox = Alert(Alert.AlertType.ERROR)

        alertBox.title = "Connection lost"
        alertBox.contentText = "SSL Connection lost, please reconnect."
        alertBox.initOwner(rootContainer.scene.window)
        alertBox.buttonTypes.setAll(ButtonType.OK)

        alertBox.showAndWait()

        (rootContainer.scene.window as Stage).close()
    }

    override fun onPacketReceived(packet: NetworkPacket, sslThread: SSLThread) {

    }

    override fun onPacketReceiveError(exception: Exception, sslThread: SSLThread) {
        return
    }

    override fun onEventOccurred(sslThread: SSLThread, eventId: SSLThreadEvent, exception: java.lang.Exception?) {
        if (exception != null) {
            Platform.runLater {
                onConnectionLost()
            }
        }
    }
}