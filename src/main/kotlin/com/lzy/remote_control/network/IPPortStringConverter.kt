package com.lzy.remote_control.network

import javafx.util.StringConverter
import java.security.InvalidParameterException

class IPPortStringConverter: StringConverter<IPPort>() {
    override fun toString(`object`: IPPort?): String {
        if (`object` == null) return ""

        return "IP = ${`object`.ip}, PORT = ${`object`.port}"
    }

    override fun fromString(string: String?): IPPort {
        if (string == null) return IPPort("", 0)

        val finalString = string.replace("IP = ", "")
                                .replace("PORT = ", "")
                                .replace(" ", "")

        try {
            val words = finalString.split(',')

            if (words.size != 2)
                throw InvalidParameterException("String is invalid.")

            val port = Integer.parseInt(words[1])

            return IPPort(words[0], port)
        } catch (_: Exception) {
            return IPPort("", 0)
        }
    }
}