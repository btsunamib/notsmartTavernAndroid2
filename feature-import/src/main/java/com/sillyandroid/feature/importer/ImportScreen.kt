package com.sillyandroid.feature.importer

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sillyandroid.core.model.ImportConflictMode

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    readBytes: (Uri) -> Pair<String, ByteArray>?,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val worlds by viewModel.worldBooks.collectAsStateWithLifecycle()
    val regex by viewModel.regexSets.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val extensions by viewModel.extensions.collectAsStateWithLifecycle()
    val themes by viewModel.themes.collectAsStateWithLifecycle()
    val selectedThemeId by viewModel.selectedThemeId.collectAsStateWithLifecycle()
    val conflictMode by viewModel.importConflictMode.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            readBytes(uri)?.let { (name, bytes) ->
                viewModel.importBytes(name, bytes)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("导入中心（PNG角色卡/世界书/正则/预设/扩展/主题）", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch("*/*") }) {
                Text("选择文件导入")
            }
        }

        ConflictModeCard(
            mode = conflictMode,
            onChange = viewModel::setConflictMode,
        )

        Text(uiState.lastMessage)
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionCard("角色卡", characters.map { it.name })
            }
            item {
                SectionCard("世界书", worlds.map { it.name })
            }
            item {
                SectionCard("正则", regex.map { it.name })
            }
            item {
                SectionCard("预设", presets.map { it.name })
            }
            item {
                ExtensionCard(
                    items = extensions,
                    onToggle = viewModel::toggleExtension,
                )
            }
            item {
                ThemeCard(
                    items = themes,
                    selectedThemeId = selectedThemeId,
                    onApply = viewModel::applyTheme,
                )
            }
        }
    }
}

@Composable
private fun ConflictModeCard(
    mode: ImportConflictMode,
    onChange: (ImportConflictMode) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("重名冲突策略", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioButton(
                    selected = mode == ImportConflictMode.Rename,
                    onClick = { onChange(ImportConflictMode.Rename) },
                )
                Text("重命名")
                RadioButton(
                    selected = mode == ImportConflictMode.Overwrite,
                    onClick = { onChange(ImportConflictMode.Overwrite) },
                )
                Text("覆盖")
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, items: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (items.isEmpty()) {
                Text("暂无")
            } else {
                items.forEach { Text("- $it") }
            }
        }
    }
}

@Composable
private fun ExtensionCard(
    items: List<com.sillyandroid.core.model.ExtensionPackage>,
    onToggle: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("扩展", style = MaterialTheme.typography.titleSmall)
            if (items.isEmpty()) {
                Text("暂无")
            } else {
                items.forEach { ext ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${ext.name} (${if (ext.enabled) "已启用" else "未启用"})")
                    }
                    if (ext.permissions.isNotEmpty()) {
                        Text("权限: ${ext.permissions.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (ext.hooks.isNotEmpty()) {
                        Text("钩子: ${ext.hooks.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onToggle(ext.id) }) {
                            Text(if (ext.enabled) "停用" else "启用")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    items: List<com.sillyandroid.core.model.ThemeConfig>,
    selectedThemeId: String?,
    onApply: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("主题", style = MaterialTheme.typography.titleSmall)
            if (items.isEmpty()) {
                Text("暂无")
            } else {
                items.forEach { theme ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${theme.name}${if (theme.id == selectedThemeId) "（当前）" else ""}")
                        Button(onClick = { onApply(theme.id) }) {
                            Text("应用")
                        }
                    }
                }
            }
        }
    }
}

fun readFileBytesFromUri(context: Context, uri: Uri): Pair<String, ByteArray>? {
    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return name to bytes
}
