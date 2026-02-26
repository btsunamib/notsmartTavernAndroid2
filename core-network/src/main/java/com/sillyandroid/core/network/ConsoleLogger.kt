package com.sillyandroid.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ConsoleLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    fun log(message: String) {
        val line = "${System.currentTimeMillis()} | $message"
        _logs.update { old -> (old + line).takeLast(800) }
    }

    fun exportPlainText(): String = logs.value.joinToString(separator = "\n")
}
