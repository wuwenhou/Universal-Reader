package com.universalreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalreader.app.BuildConfig
import com.universalreader.app.browser.BrowserUiState
import com.universalreader.app.browser.BrowserViewModel
import com.universalreader.app.tts.PlaybackSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: BrowserViewModel, state: BrowserUiState) {
    val tts by PlaybackSession.state.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cài đặt") },
                    navigationIcon = {
                        TextButton(onClick = viewModel::closeOverlay) { Text("Quay lại") }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingSwitch(
                    title = "Chặn quảng cáo",
                    description = "Bộ lọc host quảng cáo và CSS thủ công. Có ngoại lệ theo site.",
                    checked = state.adBlockEnabled,
                    onCheckedChange = viewModel::setAdBlockEnabled
                )
                SettingSwitch(
                    title = "Tự động chuyển chương",
                    description = "Khi TTS đọc xong, mở chương tiếp theo nếu trang có liên kết “next”.",
                    checked = state.autoNextChapter,
                    onCheckedChange = viewModel::setAutoNextChapter
                )

                HorizontalDivider()

                Text(
                    "Ngoại lệ quảng cáo cho site hiện tại: " +
                        if (state.adBlockException) "đang cho phép" else "đang chặn",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = viewModel::toggleAdsForCurrentSite) {
                    Text(
                        if (state.adBlockException) "Chặn quảng cáo trên site này"
                        else "Cho phép quảng cáo trên site này"
                    )
                }
                OutlinedButton(onClick = viewModel::clearAdBlockExceptions) {
                    Text("Xóa tất cả ngoại lệ")
                }

                HorizontalDivider()

                Text(
                    "Lịch sử chống lặp chương: ${viewModel.chapterHistorySize()} URL",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = viewModel::clearChapterHistory) {
                    Text("Xóa lịch sử chương")
                }

                HorizontalDivider()

                Text(
                    "Native core: v${state.coreVersion} · " +
                        if (state.nativeAvailable) "JNI OK" else "fallback Kotlin",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "TTS: ${tts.voiceName ?: "chưa khởi tạo"} · " +
                        if (tts.voiceOffline == true) "offline" else "có thể cần mạng",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Universal Reader ${BuildConfig.VERSION_NAME} · MIT",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
