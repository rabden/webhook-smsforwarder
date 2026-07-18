package com.rabden.smsforwarder.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimestamp(
    context: Context,
    timestamp: Long,
    pattern24h: String,
    pattern12h: String
): String {
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) pattern24h else pattern12h
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(timestamp))
}
