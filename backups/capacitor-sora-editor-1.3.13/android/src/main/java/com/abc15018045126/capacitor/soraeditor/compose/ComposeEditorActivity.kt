package com.abc15018045126.capacitor.soraeditor.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.abc15018045126.capacitor.soraeditor.compose.ui.EditorScreen
import com.abc15018045126.capacitor.soraeditor.compose.ui.theme.NotesTheme

class ComposeEditorActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get file path from intent
        val filePath = intent.getStringExtra("FILE_PATH") ?: ""
        val autoFocus = intent.getBooleanExtra("AUTO_FOCUS", false)
        android.util.Log.d("ComposeEditorActivity", "onCreate with filePath: $filePath, autoFocus: $autoFocus")
        
        if (filePath.isNotEmpty()) {
            viewModel.loadFile(this, filePath, autoFocus)
        } else {
            android.util.Log.e("ComposeEditorActivity", "No file path provided")
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            
            LaunchedEffect(uiState.keyboardAdjust) {
                if (uiState.keyboardAdjust) {
                    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                } else {
                    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                }
            }

            NotesTheme(darkTheme = uiState.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Auto-save logic is handled inside saveOnExit, including rename check on exit
        viewModel.saveOnExit(this)
    }
}
