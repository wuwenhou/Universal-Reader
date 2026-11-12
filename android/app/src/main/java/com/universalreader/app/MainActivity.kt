package com.universalreader.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.universalreader.app.core.NativeBridge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val coreVersion = remember {
                runCatching { NativeBridge.coreVersion() }.getOrElse { -1 }
            }
            var address by remember { mutableStateOf("https://example.org") }
            var target by remember { mutableStateOf("https://example.org") }
            var readerText by remember { mutableStateOf("") }
            var webView by remember { mutableStateOf<WebView?>(null) }
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "Universal Reader · Prototype · core v$coreVersion",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
                        TextField(address, { address = it }, Modifier.weight(1f), singleLine = true, label = { Text("URL") })
                        Button(onClick = {
                            val candidate = address.trim()
                            if (candidate.startsWith("https://") || candidate.startsWith("http://")) target = candidate
                        }) { Text("Go") }
                    }
                    Row {
                        Button(onClick = {
                            webView?.evaluateJavascript("(document.querySelector('article') || document.body).innerText") { raw ->
                                // JSON-encoded result; decode instead of stripping quote characters.
                                readerText = try { org.json.JSONTokener(raw).nextValue() as? String ?: "" } catch (_: Exception) { "" }
                            }
                        }) { Text("Read page") }
                        if (readerText.isNotEmpty()) {
                            Button(onClick = { readerText = "" }) { Text("Browser") }
                        }
                    }
                    if (readerText.isNotEmpty()) {
                        Text(readerText, Modifier.weight(1f))
                    } else {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    webViewClient = WebViewClient()
                                    webView = this
                                    loadUrl(target)
                                }
                            },
                            update = { view -> if (view.url != target) view.loadUrl(target) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
