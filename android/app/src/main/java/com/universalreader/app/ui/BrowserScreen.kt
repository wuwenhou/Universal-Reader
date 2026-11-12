package com.universalreader.app.ui

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalreader.app.browser.BrowserViewModel
import com.universalreader.app.browser.ReaderChromeClient
import com.universalreader.app.browser.ReaderWebViewClient
import com.universalreader.app.browser.WebViewConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Universal Reader",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.title.isNotBlank()) {
                            Text(
                                state.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.openReader() }) { Text("Đọc") }
                    TextButton(onClick = viewModel::openLibrary) { Text("Thư viện") }
                    TextButton(onClick = viewModel::openSettings) { Text("Cài đặt") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AddressBar(
                address = state.address,
                onAddressChange = viewModel::onAddressChange,
                onGo = {
                    viewModel.navigate(state.address)
                    focusManager.clearFocus()
                }
            )

            if (state.loading) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(Modifier.height(4.dp))
            }

            Box(Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            WebViewConfig.apply(this)
                            webViewClient = ReaderWebViewClient(
                                object : ReaderWebViewClient.Callbacks {
                                    override fun onPageStarted(url: String) =
                                        viewModel.onPageStarted(url)

                                    override fun onPageFinished(url: String) =
                                        viewModel.onPageFinished(url)

                                    override fun onUrlChanged(url: String) =
                                        viewModel.onUrlChanged(url)

                                    override fun onPageError(message: String) =
                                        viewModel.onPageError(message)

                                    override fun onBlockedUrl(url: String) =
                                        viewModel.onBlockedUrl(url)

                                    override fun shouldBlockRequest(url: String): Boolean =
                                        viewModel.shouldBlockRequest(url)
                                }
                            )
                            webChromeClient = ReaderChromeClient(
                                onProgress = viewModel::onProgress,
                                onTitle = viewModel::onPageTitle
                            )
                            viewModel.attach(this)
                            viewModel.restoreOrLoad(this)
                        }.also { webView = it }
                    },
                    onRelease = { view ->
                        viewModel.detach(view)
                        view.destroy()
                        if (webView === view) webView = null
                    }
                )

                if (state.readerLoading) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Đang trích xuất nội dung…")
                            }
                        }
                    }
                }
            }

            state.error?.let { message ->
                ErrorBar(
                    message = message,
                    onReadSelection = viewModel::readSelection,
                    onDismiss = viewModel::clearError
                )
            }
        }
    }
}

@Composable
private fun AddressBar(
    address: String,
    onAddressChange: (String) -> Unit,
    onGo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text("Địa chỉ HTTPS") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { onGo() })
        )
        TextButton(onClick = onGo) { Text("Đi") }
    }
}

@Composable
private fun ErrorBar(
    message: String,
    onReadSelection: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onReadSelection) { Text("Đọc đoạn đã chọn") }
                TextButton(onClick = onDismiss) { Text("Đóng") }
            }
        }
    }
}
