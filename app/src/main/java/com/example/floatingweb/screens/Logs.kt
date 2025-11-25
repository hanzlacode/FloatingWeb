package com.example.floatingweb.screens

import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CrashLogger {

    private val logs = mutableStateListOf<String>()
    private var initialized = false
    private const val LOG_FILE = "app_crash_log.txt"

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        loadFromFile(context)
        setupCrashHandler(context)
    }

    fun log(context: Context, tag: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val logLine = "$timestamp $tag: $message"
        logs.add(logLine)
        saveToFile(context, logLine)
    }

    fun getLogs(): List<String> = logs.toList()

    private fun loadFromFile(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE)
            if (file.exists()) {
                logs.addAll(file.readLines())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToFile(context: Context, line: String) {
        try {
            val file = File(context.filesDir, LOG_FILE)
            file.appendText(line + "\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupCrashHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()
            log(context, "CRASH", stackTrace)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun clearLogs(context: Context) {
        logs.clear()
        try {
            val file = File(context.filesDir, LOG_FILE)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

@Composable
fun LogsScreen(context: Context) {
    CrashLogger.init(context)

    var searchText by remember { mutableStateOf("") }
    val logs by remember { derivedStateOf { CrashLogger.getLogs() } }
    val scrollState = rememberScrollState()

    LaunchedEffect(logs) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val filteredLogs = if (searchText.isBlank()) logs
    else logs.filter { it.contains(searchText, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Search logs...") },
                singleLine = true
            )

            Button(
                onClick = {
                    // Clear in-memory logs
                    CrashLogger.clearLogs(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(Color.RED))
            ) {
                Text("Remove All", color = androidx.compose.ui.graphics.Color(Color.WHITE))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val listState = rememberLazyListState()
        SelectionContainer {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                items(filteredLogs) { logLine ->
                    LogLineText(logLine)
                }
            }
        }
        var scrolled = false
        LaunchedEffect(filteredLogs) {
            if (filteredLogs.isNotEmpty() && !scrolled) {
                listState.scrollToItem(filteredLogs.size - 1)
                scrolled = true
            }
        }
    }
}

@Composable
fun LogLineText(logLine: String) {
    val color = when {
        logLine.contains("CRASH") || logLine.contains("Exception", ignoreCase = true) || logLine.contains("❌") ->
            androidx.compose.ui.graphics.Color(0xFFF44336)   // Red for errors
        logLine.contains("⚠️") || logLine.contains("WARN") ->
            androidx.compose.ui.graphics.Color(0xFFFF9800)   // Orange for warnings
        logLine.contains("DEBUG") || logLine.contains("D ") ->
            androidx.compose.ui.graphics.Color(0xFF2196F3)   // Blue for debug/info
        logLine.contains("✅") ->
            androidx.compose.ui.graphics.Color(0xFF4CAF50)   // Green for success
        else -> androidx.compose.ui.graphics.Color(0xFFDDDDDD) // Light gray
    }

    val bgColor = if (logLine.contains("onStartCommand", ignoreCase = true))
        androidx.compose.ui.graphics.Color(0xFF212F3D) // A strong blue-gray for onStartCommand calls
    else
        androidx.compose.ui.graphics.Color.Transparent

    Box(modifier = Modifier
        .fillMaxWidth()
        .background(bgColor)
    ) {
        Text(
            text = logLine,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        )
    }
    Spacer(modifier = Modifier.height(2.dp))
}
