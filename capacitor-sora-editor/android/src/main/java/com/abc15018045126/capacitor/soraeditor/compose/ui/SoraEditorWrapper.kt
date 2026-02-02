package com.abc15018045126.capacitor.soraeditor.compose.ui

import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

@Composable
fun SoraEditorView(
    content: String,
    onContentChange: (String) -> Unit,
    fontSize: Float = 18f,
    showLineNumbers: Boolean = true,
    wordWrap: Boolean = false,
    editable: Boolean = true,
    backgroundColor: String = "#FFFFFF",
    modifier: Modifier = Modifier,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {}
) {
    var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }
    
    // Expose undo/redo functions
    LaunchedEffect(editorInstance) {
        editorInstance?.let { editor ->
            // Store reference for external calls
        }
    }
    
    AndroidView(
        factory = { context ->
            CodeEditor(context).apply {
                setTextSize(fontSize)
                setTypefaceText(Typeface.MONOSPACE)
                isLineNumberEnabled = showLineNumbers
                isWordwrap = wordWrap
                isEditable = editable
                setText(content)
                
                // Set background color
                try {
                    val color = Color.parseColor(backgroundColor)
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                    
                    colorScheme?.setColor(EditorColorScheme.WHOLE_BACKGROUND, color)
                    colorScheme?.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, color)
                    
                    if (luminance < 0.5) {
                        colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, Color.WHITE)
                        colorScheme?.setColor(EditorColorScheme.LINE_NUMBER, Color.GRAY)
                    } else {
                        colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, Color.BLACK)
                        colorScheme?.setColor(EditorColorScheme.LINE_NUMBER, Color.DKGRAY)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                editorInstance = this
            }
        },
        update = { view ->
            view.setTextSize(fontSize)
            view.isLineNumberEnabled = showLineNumbers
            view.isWordwrap = wordWrap
            view.isEditable = editable
            
            // Only update text if it's different to avoid cursor jump
            if (view.text.toString() != content) {
                view.setText(content)
            }
            
            // Update background color
            try {
                val color = Color.parseColor(backgroundColor)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                
                view.colorScheme?.setColor(EditorColorScheme.WHOLE_BACKGROUND, color)
                view.colorScheme?.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, color)
                
                if (luminance < 0.5) {
                    view.colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, Color.WHITE)
                    view.colorScheme?.setColor(EditorColorScheme.LINE_NUMBER, Color.GRAY)
                } else {
                    view.colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, Color.BLACK)
                    view.colorScheme?.setColor(EditorColorScheme.LINE_NUMBER, Color.DKGRAY)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
