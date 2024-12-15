package com.lzy.remote_control.network

import java.lang.Exception

interface SSLThreadEventCallback {
    fun onEventOccurred(sslThread: SSLThread, eventId: SSLThreadEvent, exception: Exception?)
}