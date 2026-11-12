package com.universalreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalreader.app.browser.BrowserViewModel
import com.universalreader.app.reader.ExtractionSource
import com.universalreader.app.reader.ReaderContent
import com.universalreader.app.tts.PlaybackSession
import com.universalreader.app.tts.TtsUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(viewModel: BrowserViewModel, content: ReaderContent) {
    val tts by PlaybackSession.state.collectAsStateWithLifecycle()
    val browserState by viewModel.state.collectAsStateWithLifecycle()
    val fontScale = browserState.fontScale

    Surface(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                content.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                buildString {
                                    append(content.siteName ?: sourceLabel(content.source))
                                    append(" · ")
                                    append("${content.wordCount} từ")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = viewModel::closeReader) { Text("Quay lại") }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.setFontScale(fontScale - 0.1f) }) {
                            Text("A−")
                        }
                        TextButton(onClick = { viewModel.setFontScale(fontScale + 0.1f) }) {
                            Text("A+")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Column {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${content.paragraphs.size} đoạn · ${sourceLabel(content.source)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { viewModel.nextChapter() },
                                enabled = content.nextUrl != null
                            ) {
                                Text(
                                    if (content.nextUrl != null) "Chương tiếp theo"
                                    else "Không có chương sau"
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TtsPanel(tts, viewModel)
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(content.paragraphs) { _, paragraph ->
                            Text(
                                paragraph,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = (18 * fontScale).sp,
                                    lineHeight = (28 * fontScale).sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TtsPanel(tts: TtsUiState, viewModel: BrowserViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (tts.isPlaying) viewModel.pauseTts() else viewModel.startTts()
                }
            ) {
                Text(
                    when {
                        tts.isPlaying -> "Tạm dừng"
                        tts.isPreparing -> "Đang chuẩn bị…"
                        else -> "Đọc"
                    }
                )
            }
            OutlinedButton(onClick = viewModel::stopTts) { Text("Dừng") }
            OutlinedButton(onClick = viewModel::previousTts) { Text("Câu trước") }
            OutlinedButton(onClick = viewModel::nextTts) { Text("Câu sau") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tốc độ " + String.format(Locale.US, "%.1f", tts.rate) + "×",
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = tts.rate,
                onValueChange = viewModel::setTtsRate,
                valueRange = 0.5f..2.0f,
                modifier = Modifier.weight(1f)
            )
        }

        val details = buildList {
            if (tts.total > 0) add("Câu ${tts.currentIndex + 1}/${tts.total}")
            tts.voiceName?.let { name ->
                add(
                    "Giọng " + name.substringAfterLast('/') +
                        if (tts.voiceOffline == true) " (offline)" else " (có thể cần mạng)"
                )
            }
        }
        if (details.isNotEmpty()) {
            Text(
                details.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (tts.completed) {
            Text(
                "Đã đọc xong chương này.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        tts.error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun sourceLabel(source: ExtractionSource): String = when (source) {
    ExtractionSource.SITE_ADAPTER -> "adapter"
    ExtractionSource.READABILITY -> "Readability"
    ExtractionSource.SELECTION -> "đoạn chọn"
}
