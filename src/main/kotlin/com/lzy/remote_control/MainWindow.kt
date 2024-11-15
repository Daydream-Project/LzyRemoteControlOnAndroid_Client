package com.lzy.remote_control

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.TextArea
import javafx.scene.input.MouseEvent

class MainWindow {
    @FXML
    private lateinit var logBox: TextArea
    @FXML
    private lateinit var deviceList: ListView<String>
    @FXML
    private lateinit var findDeviceBtn: Button

    private var isFindingDevice = false

    private var thread: FindDeviceThread? = null

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

    fun stopFindDevice() {
        if (thread != null) {
            thread!!.terminate()
            while (thread!!.state != Thread.State.TERMINATED) continue;
            thread = null
        }
    }

    fun startFindDevice() {
        if (thread == null) {
            thread = FindDeviceThread(deviceList)
            thread!!.start()
        }
    }
}