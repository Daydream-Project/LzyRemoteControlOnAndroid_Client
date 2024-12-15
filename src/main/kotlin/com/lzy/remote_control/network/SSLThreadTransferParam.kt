package com.lzy.remote_control.network

class SSLThreadTransferParam(var buffer: ByteArray, var length: Int, var offset: Int, var isReceive: Boolean, var callback: SSLThreadTransferCallback)