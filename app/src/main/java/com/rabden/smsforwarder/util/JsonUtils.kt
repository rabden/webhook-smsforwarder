package com.rabden.smsforwarder.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun parseHeaders(json: String): Map<String, String> {
    return try {
        val type = object : TypeToken<Map<String, String>>() {}.type
        Gson().fromJson(json, type) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
}
