package com.anhdaden

import com.lagradost.cloudstream3.app

suspend fun isUrlValid(url: String, timeout: Long = 3): Boolean {
    return try {
        val response = app.head(url, timeout = timeout)
        response.code < 400
    } catch (e: Exception) {
        false
    }
}
