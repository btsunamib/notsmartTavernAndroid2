package com.sillyandroid.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sillyandroid.core.model.Role

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("聊天（支持导入联动）", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = uiState.providerConfig.baseUrl,
            onValueChange = { viewModel.updateProvider(it, uiState.providerConfig.apiKey, uiState.providerConfig.model) },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )

        OutlinedTextField(
            value = uiState.providerConfig.apiKey,
            onValueChange = { viewModel.updateProvider(uiState.providerConfig.baseUrl, it, uiState.providerConfig.model) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = uiState.providerConfig.model,
            onValueChange = { viewModel.updateProvider(uiState.providerConfig.baseUrl, uiState.providerConfig.apiKey, it) },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = uiState.persona,
            onValueChange = viewModel::updatePersona,
            label = { Text("用户 Persona") },
            modifier = Modifier.fillMaxWidth(),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("已导入角色选择", style = MaterialTheme.typography.labelLarge)
                if (characters.isEmpty()) {
                    Text("暂无角色，请先在导入页导入 PNG 角色卡")
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        characters.take(3).forEach { ch ->
                            Button(onClick = { viewModel.selectCharacter(ch.id) }) {
                                Text(ch.name)
                            }
                        }
                    }
                }

                Text("已导入预设选择", style = MaterialTheme.typography.labelLarge)
                if (presets.isEmpty()) {
                    Text("暂无预设，请先在导入页导入 JSON 预设")
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.take(3).forEach { preset ->
                            Button(onClick = { viewModel.selectPreset(preset.id) }) {
                                Text(preset.name)
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::newChat) {
                Text("新聊天")
            }
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(uiState.messages, key = { it.id }) { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = when (msg.role) {
                                Role.System -> "System"
                                Role.User -> "User"
                                Role.Assistant -> "Assistant"
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(msg.content)
                    }
                }
            }
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = uiState.input,
            onValueChange = viewModel::updateInput,
            label = { Text("输入消息") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = viewModel::send,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
        ) {
            Text("发送")
        }
    }
}
