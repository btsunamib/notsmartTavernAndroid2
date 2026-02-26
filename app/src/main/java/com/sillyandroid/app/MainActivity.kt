package com.sillyandroid.app

import android.app.Application
import android.graphics.Color.parseColor
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillyandroid.core.network.ConsoleLogger
import com.sillyandroid.feature.chat.ChatScreen
import com.sillyandroid.feature.chat.ChatViewModel
import com.sillyandroid.feature.console.ConsoleScreen
import com.sillyandroid.feature.importer.ImportScreen
import com.sillyandroid.feature.importer.ImportViewModel
import com.sillyandroid.feature.importer.readFileBytesFromUri
import kotlinx.coroutines.launch

class SillyAndroidApp : Application()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val appGraph = remember { AppGraph() }
    val coordinatorViewModel: AppCoordinatorViewModel = viewModel(
        factory = AppCoordinatorViewModelFactory(appGraph),
    )
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(appGraph),
    )

    val themes by coordinatorViewModel.themes.collectAsStateWithLifecycle()
    val selectedThemeId by coordinatorViewModel.selectedThemeId.collectAsStateWithLifecycle()
    val selectedTheme = themes.firstOrNull { it.id == selectedThemeId }

    LaunchedEffect(Unit) {
        coordinatorViewModel.onAppStart()
        coordinatorViewModel.onSettingsOpen()
    }

    val colorScheme = remember(selectedThemeId, themes) {
        val primaryHex = selectedTheme?.tokens?.get("primary")
        val secondaryHex = selectedTheme?.tokens?.get("secondary")
        val backgroundHex = selectedTheme?.tokens?.get("background")

        darkColorScheme(
            primary = parseColorOr(primaryHex, Color(0xFF7D7DFF)),
            secondary = parseColorOr(secondaryHex, Color(0xFF9A8BFF)),
            background = parseColorOr(backgroundHex, Color(0xFF121212)),
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            ChatScreen(
                viewModel = chatViewModel,
                readBytes = { uri -> readFileBytesFromUri(context, uri) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun FrontendWebViewScreen(
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable { mutableStateOf("https://docs.sillytavern.app/") }
    var debugMode by rememberSaveable { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("前端 URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                webViewRef?.loadUrl(url)
                ConsoleLogger.log("frontend load url=$url")
            }) {
                Text("加载")
            }
            Text("调试")
            Switch(
                checked = debugMode,
                onCheckedChange = {
                    debugMode = it
                    scope.launch {
                        webViewRef?.settings?.javaScriptEnabled = it
                        ConsoleLogger.log("frontend debug jsEnabled=$it")
                    }
                },
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    settings.javaScriptEnabled = debugMode
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    loadUrl(url)
                    webViewRef = this
                }
            },
            update = {
                webViewRef = it
            },
        )
    }
}

private fun parseColorOr(hex: String?, fallback: Color): Color {
    return try {
        if (hex.isNullOrBlank()) fallback else Color(parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
