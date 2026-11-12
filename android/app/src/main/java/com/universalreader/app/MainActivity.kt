package com.universalreader.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalreader.app.browser.BrowserViewModel
import com.universalreader.app.browser.Screen
import com.universalreader.app.ui.BrowserScreen
import com.universalreader.app.ui.LibraryScreen
import com.universalreader.app.ui.ReaderScreen
import com.universalreader.app.ui.SettingsScreen
import com.universalreader.app.ui.UniversalReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        setContent {
            UniversalReaderTheme {
                val viewModel: BrowserViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                BackHandler {
                    if (!viewModel.handleBack()) finish()
                }

                Box(Modifier.fillMaxSize()) {
                    // The browser stays composed underneath overlays so the
                    // WebView survives reader mode and rotation.
                    BrowserScreen(viewModel)

                    when (state.screen) {
                        Screen.READER -> state.reader?.let { ReaderScreen(viewModel, it) }
                        Screen.LIBRARY -> LibraryScreen(viewModel, state.library)
                        Screen.SETTINGS -> SettingsScreen(viewModel, state)
                        Screen.BROWSER -> Unit
                    }
                }
            }
        }
    }
}
