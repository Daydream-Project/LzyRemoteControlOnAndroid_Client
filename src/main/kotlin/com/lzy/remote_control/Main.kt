package com.lzy.remote_control

import javafx.application.Application
import javafx.application.Application.launch
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class KotlinJavaFXApp : Application() {
    private var mainWindowObj: FXMLLoader? = null

    companion object {
        private const val mainWindowFile = "/MainWindow.fxml"
    }

    override fun start(primaryStage: Stage) {
        mainWindowObj = FXMLLoader(javaClass.getResource(mainWindowFile))
        primaryStage.title = "LzyRemoteControl Client"
        primaryStage.scene = Scene(mainWindowObj!!.load())
        primaryStage.show()
    }

    override fun stop() {
        //If mainWindow is load, free resources.
        if (mainWindowObj != null) {
            mainWindowObj!!.getController<MainWindow>().stopFindDevice()
        }
    }
}

fun main(args: Array<String>) {
    launch(KotlinJavaFXApp::class.java, *args)
}