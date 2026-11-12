package com.universalreader.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.universalreader.app.browser.BrowserViewModel
import com.universalreader.app.data.BookRecord
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: BrowserViewModel, books: List<BookRecord>) {
    Surface(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Thư viện") },
                    navigationIcon = {
                        TextButton(onClick = viewModel::closeOverlay) { Text("Quay lại") }
                    },
                    actions = {
                        if (books.isNotEmpty()) {
                            TextButton(onClick = viewModel::clearLibrary) { Text("Xóa hết") }
                        }
                    }
                )
            }
        ) { padding ->
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chưa có sách. Mở một trang, bấm “Đọc” rồi quay lại đây.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(Modifier.padding(padding)) {
                    items(books, key = { it.url }) { book ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    book.title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        buildString {
                                            append(book.siteName ?: "Web")
                                            append(" · ")
                                            append(book.percent)
                                            append("% · ")
                                            append(book.progress)
                                            append("/")
                                            append(book.total)
                                            append(" câu")
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        DateFormat.getDateTimeInstance()
                                            .format(Date(book.lastReadAt)),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            trailingContent = {
                                TextButton(onClick = { viewModel.deleteBook(book) }) {
                                    Text("Xóa")
                                }
                            },
                            modifier = Modifier.clickable { viewModel.openBook(book) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
