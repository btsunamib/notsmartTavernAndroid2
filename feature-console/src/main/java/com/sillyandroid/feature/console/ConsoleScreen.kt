package com.sillyandroid.feature.console

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sillyandroid.core.network.ConsoleLogger
import java.io.File

@Composable
fun ConsoleScreen(
    modifier: Modifier = Modifier,
    context: Context? = null,
) {
    val logs by ConsoleLogger.logs.collectAsStateWithLifecycle(initialValue = emptyList<String>())
    var keyword by remember { mutableStateOf("") }

    val filteredLogs = logs.filter {
        keyword.isBlank() || it.contains(keyword, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("控制台（请求/原始输出）", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("过滤关键字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        val exportPreview = filteredLogs.joinToString(separator = "\n")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { ConsoleLogger.log("export preview chars=${exportPreview.length}") }) {
                Text("记录导出长度")
            }
            Button(onClick = {
                if (context == null) {
                    ConsoleLogger.log("export file skipped: no context")
                } else {
                    val file = exportLogsToFile(context, exportPreview)
                    ConsoleLogger.log("logs exported path=${file.absolutePath}")
                }
            }) {
                Text("导出到文件")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(filteredLogs.reversed(), key = { it.hashCode() }) { line ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = line,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun exportLogsToFile(context: Context, content: String): File {
    val dir = File(context.filesDir, "exports")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "console-${System.currentTimeMillis()}.log")
    file.writeText(content)
    return file
}
