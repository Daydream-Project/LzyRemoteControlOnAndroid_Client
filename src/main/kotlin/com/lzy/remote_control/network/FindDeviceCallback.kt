package com.lzy.remote_control.network

interface FindDeviceCallback {
    fun onDeviceFound(ip: String, port: Int)
}