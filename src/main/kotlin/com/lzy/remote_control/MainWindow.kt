package com.lzy.remote_control

import com.lzy.remote_control.network.FindDeviceCallback
import com.lzy.remote_control.network.FindDeviceThread
import com.lzy.remote_control.network.IPPort
import com.lzy.remote_control.network.IPisV6
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.stage.Stage

class MainWindow: FindDeviceCallback {
    @FXML
    private lateinit var ipTypeBox: ComboBox<String>
    @FXML
    private lateinit var logBox: TextArea
    @FXML
    private lateinit var deviceList: ListView<IPPort>
    @FXML
    private lateinit var findDeviceBtn: Button

    private var isFindingDevice = false

    private var thread: FindDeviceThread? = null

    private val deviceInfos: MutableList<IPPort> = mutableListOf()

    companion object {
        private const val DEVICE_CONTROL_FILE = "/DeviceControl.fxml"
    }

    @FXML
    private fun initialize() {
        if (ipTypeBox.items.size == 0)
            ipTypeBox.items.setAll("IPV4", "IPV6", "ALL")
    }

    @FXML
    fun findDeviceBtnOnClick(mouseEvent: MouseEvent) {
        if (isFindingDevice) {
            findDeviceBtn.text = "Find Device."

            stopFindDevice()

        } else {
            findDeviceBtn.text = "Stop Find Device."

            deviceList.items.clear()

            startFindDevice()
        }
        isFindingDevice = !isFindingDevice
    }

    @FXML
    fun onIPTypeBoxValueSelected(actionEvent: ActionEvent) {
        val ipType = ipTypeBox.selectionModel.selectedItem

        deviceList.items.clear()

        when (ipType) {
            "IPV4" -> {
                for (ipPort in deviceInfos) {
                    if (!IPisV6(ipPort.ip))
                        deviceList.items.add(ipPort)
                }
            }
            "IPV6" -> {
                for (ipPort in deviceInfos) {
                    if (IPisV6(ipPort.ip))
                        deviceList.items.add(ipPort)
                }
            }
            "ALL" -> {
                for (ipPort in deviceInfos)
                    deviceList.items.add(ipPort)
            }
        }
    }

    @FXML
    fun onConnect(actionEvent: ActionEvent) {
        val ipPort = deviceList.selectionModel.selectedItem

        if (ipPort != null) {
            val fxmlLoader = FXMLLoader(javaClass.getResource(DEVICE_CONTROL_FILE))
            val stage = Stage()

            fxmlLoader.setControllerFactory { DeviceControl(ipPort) }

            stage.title = "IP = ${ipPort.ip} PORT = ${ipPort.port}"
            stage.scene = Scene(fxmlLoader.load())

            stage.show()
        } else {
            val alertBox = Alert(Alert.AlertType.INFORMATION)
            alertBox.buttonTypes.setAll(ButtonType.OK)
            alertBox.title = "Information"
            alertBox.contentText = "Please select a device."
            alertBox.initOwner(ipTypeBox.scene.window)
            alertBox.showAndWait()
        }
    }

    fun stopFindDevice() {
        if (thread != null) {
            ipTypeBox.isDisable = false
            thread!!.terminate()
            while (thread!!.state != Thread.State.TERMINATED) continue
            thread = null
        }
    }

    fun startFindDevice() {
        if (thread == null) {
            ipTypeBox.value = ""
            ipTypeBox.isDisable = true
            deviceInfos.clear()
            deviceList.items.clear()
            thread = FindDeviceThread(this)
            thread!!.start()
        }
    }

    override fun onDeviceFound(ip: String, port: Int) {
        Platform.runLater {
            val ipPort = IPPort(ip, port)
            deviceInfos.add(ipPort)
            deviceList.items.add(ipPort)
        }
    }
}