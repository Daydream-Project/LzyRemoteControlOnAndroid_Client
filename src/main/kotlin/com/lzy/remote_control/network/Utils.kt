package com.lzy.remote_control.network

//CHeck a ip address is IPV6 version.
fun IPisV6(ip: String): Boolean {
    return ip.find { value ->
        value == 'a' || value == 'b' || value == 'c' || value == 'd' || value == 'e' || value == 'f' ||
                value == 'A' || value == 'B' || value == 'C' || value == 'D' || value == 'E' || value == 'F' ||
                value == ':'
    } != null
}