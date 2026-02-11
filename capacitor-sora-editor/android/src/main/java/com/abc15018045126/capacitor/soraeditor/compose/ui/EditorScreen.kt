package com.abc15018045126.capacitor.soraeditor.compose.ui

import android.graphics.Typeface
import android.view.View
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import com.abc15018045126.capacitor.soraeditor.compose.EditorUiState
import com.abc15018045126.capacitor.soraeditor.compose.EditorViewModel
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.style.builtin.HandleStyleDrop
import io.github.abc15018045126.sora.widget.style.builtin.HandleStyleSideDrop
import io.github.abc15018045126.sora.widget.style.builtin.HandleStyleNone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner as ComposeLifecycleOwner

class EditorControl {
    private var editor: CodeEditor? = null

    fun attach(editor: CodeEditor) {
        this.editor = editor
    }

    fun jumpTo(pos: Int) {
        if (editor == null) return 
        editor?.let {
            if (it.isLaidOut) {
                performJump(it, pos)
            } else {
                it.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                        it.removeOnLayoutChangeListener(this)
                        performJump(it, pos)
                    }
                })
            }
        }
    }
    
    fun getCurrentCursorPosition(): Int {
        val editor = this.editor ?: return 0
        try {
            val cursor = editor.cursor!!
            val targetLine = cursor.leftLine
            val targetCol = cursor.leftColumn

            val text = editor.text.toString()
            
            var idx = 0
            var curLine = 0
            val len = text.length
            
            while (idx < len && curLine < targetLine) {
                if (text[idx] == '\n') {
                    curLine++
                }
                idx++
            }
            return (idx + targetCol).coerceAtMost(len)
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    private fun performJump(it: CodeEditor, pos: Int) {
        try {
            it.requestFocus()
            val text = it.text.toString()
            val safePos = pos.coerceIn(0, text.length)
            
            var line = 0
            var col = 0
            for (i in 0 until safePos) {
                if (text[i] == '\n') {
                    line++
                    col = 0
                } else {
                    col++
                }
            }
            
            it.setSelection(line, col)
            it.ensureSelectionVisible()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun search(text: String, isRegex: Boolean = false, matchCase: Boolean = false, wholeWord: Boolean = false) {
        try {
            val type = if (isRegex) {
                io.github.abc15018045126.sora.widget.EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION
            } else if (wholeWord) {
                io.github.abc15018045126.sora.widget.EditorSearcher.SearchOptions.TYPE_WHOLE_WORD
            } else {
                io.github.abc15018045126.sora.widget.EditorSearcher.SearchOptions.TYPE_NORMAL
            }
            // 清理输入文本：移除换行符和其他控制字符
            val cleanedText = text.replace("\\r", "").replace("\\n", "").trim()
            if (cleanedText.isEmpty()) {
                stopSearch()
                return
            }
            editor?.searcher?.search(cleanedText, io.github.abc15018045126.sora.widget.EditorSearcher.SearchOptions(type, !matchCase))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findNext() {
        try {
            editor?.searcher?.gotoNext()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findPrevious() {
        try {
            editor?.searcher?.gotoPrevious()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun replace(text: String) {
        try {
            editor?.searcher?.replaceCurrentMatch(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun replaceAll(text: String) {
        try {
            editor?.searcher?.replaceAll(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSearch() {
        try {
            editor?.searcher?.stopSearch()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun undo() {
        editor?.undo()
    }

    fun redo() {
        editor?.redo()
    }

    fun canUndo(): Boolean = editor?.canUndo() ?: false
    fun canRedo(): Boolean = editor?.canRedo() ?: false

    fun insertText(text: String) {
        editor?.insertText(text, text.length)
    }
}

@Composable
fun SoraEditorView(
    content: String,
    onContentChange: (String) -> Unit,
    onSelectionChange: (Int, Int, Int) -> Unit = { _, _, _ -> },
    fontSize: Float = 18f,
    showLineNumbers: Boolean = true,
    wordWrap: Boolean = false,
    editable: Boolean = true,
    backgroundColor: String = "#FFFFFF",
    searchMatchBackgroundColor: String = "#FFFF00",
    modifier: Modifier = Modifier,
    control: EditorControl? = null,
    onSearchMatchesChange: (Int, Int) -> Unit = { _, _ -> },
    onScroll: () -> Unit = {},
    onTap: () -> Unit = {},
    autoFocus: Boolean = false,
    lineSpacingMultiplier: Float = 1.0f,
    lineSpacingExtra: Float = 0f,
    wrapLineSpacingMultiplier: Float = 1.0f,
    wrapLineSpacingExtra: Float = 0f,
    horizontalPadding: Float = 12f,
    highlightCurrentLine: Boolean = true,
    currentLineBackgroundColor: String = "#10000000",
    cursorColor: String = "#FF000000",
    handleColor: String = "#FF000000",
    cursorWidth: Float = 2.0f,
    handleStyle: String = "side_drop",
    fontFamily: String = "Monospace",
    scrollbarColor: String = "#A0888888",
    showScrollLineInfo: Boolean = true,
    scrollbarStyle: String = "default",
    isFastMode: Boolean = false,
    initialPreviewLines: Int = 20,
    lineNumberAlign: String = "left",
    isLineNumberRightOfDivider: Boolean = false,
    isLineNumberPinned: Boolean = false,
    lineNumberColor: String = "#FF000000",
    lineDividerColor: String = "#A0888888",
    editorTextColor: String = "auto",
    textActionMenuItems: List<String> = listOf("select_all", "copy", "paste", "long_select", "cut"),
    textActionMenuHidden: List<String> = emptyList(),
    textActionMenuBgColor: String = "auto"
) {
    var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }
    
    // Flag to prevent update loop when setting text programmatically
    val isSettingTextProgrammatically = remember { mutableStateOf(false) }
    var lastAppliedFontFamily by remember { mutableStateOf("") }
    var lastAppliedHandleStyle by remember { mutableStateOf("") }
    var lastAppliedTextActionMenuItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastAppliedTextActionMenuHidden by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastAppliedTextActionMenuBgColor by remember { mutableStateOf("") }
    var lastAppliedTextSize by remember { mutableStateOf(-1f) }
    var lastAppliedCursorWidth by remember { mutableStateOf(-1f) }
    var lastAppliedLineNumbers by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedWordWrap by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedEditable by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedHighlightCurrentLine by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedShowScrollLineInfo by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedFastMode by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedInitialPreviewLines by remember { mutableStateOf<Int?>(null) }
    var lastAppliedLineNumberAlign by remember { mutableStateOf("") }
    var lastAppliedIsLineNumberRightOfDivider by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedIsLineNumberPinned by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedLineNumberColor by remember { mutableStateOf("") }
    var lastAppliedLineDividerColor by remember { mutableStateOf("") }

    // Ensure we always have the latest callbacks even if factory is not re-run
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnContentChange by rememberUpdatedState(onContentChange)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)

    // Use LifecycleEffect to sync text back to ViewModel when app is paused
    // This fixed auto-save on exit while keeping ContentChangeEvent disabled (avoiding flicker)
    val lifecycleOwner = LocalLifecycleOwner.current
    var editorInstanceForSync by remember { mutableStateOf<CodeEditor?>(null) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                editorInstanceForSync?.let { view ->
                    val currentText = view.text.toString()
                    currentOnContentChange(currentText)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(editorInstance, control) {
        editorInstance?.let { control?.attach(it) }
    }
    
    AndroidView(
        factory = { context ->
            CodeEditor(context).apply {
                setTextSize(fontSize)
                isLineNumberEnabled = showLineNumbers
                isWordwrap = wordWrap
                isEditable = editable
                isHighlightCurrentLine = highlightCurrentLine
                setCursorWidth(cursorWidth * dpUnit / 2f)
                isDisplayLnPanel = showScrollLineInfo

                
                // Set font family
                when (fontFamily) {
                    "JetBrains Mono" -> setTypefaceText(Typeface.createFromAsset(context.assets, "JetBrainsMono-Regular.ttf"))
                    "Ubuntu" -> setTypefaceText(Typeface.createFromAsset(context.assets, "Ubuntu-Regular.ttf"))
                    "Roboto" -> setTypefaceText(Typeface.createFromAsset(context.assets, "Roboto-Regular.ttf"))
                    else -> setTypefaceText(Typeface.MONOSPACE)
                }
                
                isSettingTextProgrammatically.value = true
                setText(content)
                isSettingTextProgrammatically.value = false
                
                // Use GestureDetector for reliable tap detection
                val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        currentOnTap()
                        return true
                    }
                })
                setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    false // Return false so the editor still receives touch events for selection/scrolling
                }
                
                subscribeEvent(io.github.abc15018045126.sora.event.SelectionChangeEvent::class.java) { _, _ ->
                     val cursor = this.cursor!!
                     val line = cursor.leftLine
                     val col = cursor.leftColumn
                     
                     val textStr = text.toString()
                     var charPos = 0
                     try {
                         var curL = 0
                         var i = 0
                         while (i < textStr.length && curL < line) {
                             if (textStr[i] == '\n') curL++
                             i++
                         }
                         charPos = i + col
                     } catch (e: Exception) {}
                     
                     currentOnSelectionChange(charPos, line, col)
                     
                     if (searcher.hasQuery()) {
                         onSearchMatchesChange(searcher.currentMatchedPositionIndex + 1, searcher.matchedPositionCount)
                     }
                }

                subscribeEvent(io.github.abc15018045126.sora.event.PublishSearchResultEvent::class.java) { _, _ ->
                    onSearchMatchesChange(searcher.currentMatchedPositionIndex + 1, searcher.matchedPositionCount)
                }

                subscribeEvent(io.github.abc15018045126.sora.event.ScrollEvent::class.java) { _, _ ->
                    onScroll()
                }

                // DISABLED: ContentChangeEvent causes update loop and flicker
                // User typing -> ContentChangeEvent -> onContentChange -> React state update
                // -> update block -> setText -> layout recalculation -> FLICKER
                // The editor already has the correct text from user input, no need to update state
                /*
                subscribeEvent(io.github.abc15018045126.sora.event.ContentChangeEvent::class.java) { _, _ ->
                    // Only trigger state update if this is a real user edit, not a programmatic setText
                    if (!isSettingTextProgrammatically.value) {
                        val newText = text.toString()
                        currentOnContentChange(newText)
                    }
                }
                */

                // Apply initial spacing and padding
                setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                setWrapLineSpacing(wrapLineSpacingExtra, wrapLineSpacingMultiplier)
                val dp = dpUnit
                dividerMarginRight = horizontalPadding * dp
                extraMarginRight = horizontalPadding * dp
                setLineNumberMarginLeft(horizontalPadding * dp)

                try {
                    val color = android.graphics.Color.parseColor(backgroundColor)
                    val r = android.graphics.Color.red(color)
                    val g = android.graphics.Color.green(color)
                    val b = android.graphics.Color.blue(color)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                    
                    colorScheme?.setColor(EditorColorScheme.WHOLE_BACKGROUND, color)
                    colorScheme?.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, color)
                    
                    if (luminance < 0.5) {
                        colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.WHITE)
                        colorScheme?.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.GRAY)
                    } else {
                        colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.BLACK)
                        colorScheme?.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.DKGRAY)
                    }
                    
                    if (editorTextColor != "auto" && editorTextColor.isNotEmpty()) {
                        try {
                            colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.parseColor(editorTextColor))
                        } catch (e: Exception) {}
                    }

                    try {
                        val searchMatchColor = android.graphics.Color.parseColor(searchMatchBackgroundColor)
                        colorScheme?.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, searchMatchColor)
                    } catch (e: Exception) {
                        colorScheme?.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, 0xffffff00.toInt())
                    }

                    try {
                        val currentLineColor = android.graphics.Color.parseColor(currentLineBackgroundColor)
                        colorScheme?.setColor(EditorColorScheme.CURRENT_LINE, currentLineColor)
                    } catch (e: Exception) {
                        colorScheme?.setColor(EditorColorScheme.CURRENT_LINE, 0x10000000)
                    }

                    try {
                        val cColor = android.graphics.Color.parseColor(cursorColor)
                        colorScheme?.setColor(EditorColorScheme.SELECTION_INSERT, cColor)
                    } catch (e: Exception) {
                        colorScheme?.setColor(EditorColorScheme.SELECTION_INSERT, 0xFF000000.toInt())
                    }

                    try {
                        val hColor = android.graphics.Color.parseColor(handleColor)
                        colorScheme?.setColor(EditorColorScheme.SELECTION_HANDLE, hColor)
                    } catch (e: Exception) {
                        colorScheme?.setColor(EditorColorScheme.SELECTION_HANDLE, 0xFF000000.toInt())
                    }

                    setSelectionHandleStyle(when (handleStyle) {
                        "drop" -> HandleStyleDrop(context)
                        "none" -> HandleStyleNone()
                        else -> HandleStyleSideDrop(context)
                    })

                    try {
                        val sColor = android.graphics.Color.parseColor(scrollbarColor)
                        colorScheme?.setColor(EditorColorScheme.SCROLL_BAR_THUMB, sColor)
                        colorScheme?.setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, sColor)
                    } catch (e: Exception) {
                        colorScheme?.setColor(EditorColorScheme.SCROLL_BAR_THUMB, 0xFFA0888888.toInt())
                        colorScheme?.setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, 0xFFA0888888.toInt())
                    }

                    if (scrollbarStyle == "rounded") {
                        val sColor = try { android.graphics.Color.parseColor(scrollbarColor) } catch(e: Exception) { 0xFFA0888888.toInt() }
                        val drawable = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 100f // Large radius for fully rounded ends
                            setColor(sColor)
                        }
                        renderer?.setVerticalScrollbarThumbDrawable(drawable)
                        renderer?.setHorizontalScrollbarThumbDrawable(drawable)
                    } else {
                        renderer?.setVerticalScrollbarThumbDrawable(null)
                        renderer?.setHorizontalScrollbarThumbDrawable(null)
                    }
                    isCursorAnimationEnabled = !isFastMode
                    setInitialPreviewLines(initialPreviewLines)
                    setLineNumberPaintAlign(if (lineNumberAlign == "right") android.graphics.Paint.Align.RIGHT else if (lineNumberAlign == "center") android.graphics.Paint.Align.CENTER else android.graphics.Paint.Align.LEFT)
                    setLineNumberRightOfDivider(isLineNumberRightOfDivider)
                    setPinLineNumber(isLineNumberPinned)
                    colorScheme.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.parseColor(lineNumberColor))
                    colorScheme.setColor(EditorColorScheme.LINE_DIVIDER, android.graphics.Color.parseColor(lineDividerColor))
                } catch (e: Exception) {}
                
                editorInstance = this
                editorInstanceForSync = this
                control?.attach(this)
                
                // Auto-focus if requested (for new notes)
                if (autoFocus) {
                    postDelayed({
                        requestFocus()
                        // Show keyboard using input method manager
                        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }, 200) // Delay to ensure view is fully laid out
                }
            }
        },
        update = { view ->
            if (lastAppliedTextSize != fontSize) {
                view.setTextSize(fontSize)
                lastAppliedTextSize = fontSize
            }
            if (lastAppliedLineNumbers != showLineNumbers) {
                view.isLineNumberEnabled = showLineNumbers
                lastAppliedLineNumbers = showLineNumbers
            }
            if (lastAppliedWordWrap != wordWrap) {
                view.isWordwrap = wordWrap
                lastAppliedWordWrap = wordWrap
            }
            if (lastAppliedEditable != editable) {
                view.isEditable = editable
                lastAppliedEditable = editable
            }
            if (lastAppliedHighlightCurrentLine != highlightCurrentLine) {
                view.isHighlightCurrentLine = highlightCurrentLine
                lastAppliedHighlightCurrentLine = highlightCurrentLine
            }
            val targetCursorWidth = cursorWidth * view.dpUnit / 2f
            if (lastAppliedCursorWidth != targetCursorWidth) {
                view.setCursorWidth(targetCursorWidth)
                lastAppliedCursorWidth = targetCursorWidth
            }
            if (lastAppliedShowScrollLineInfo != showScrollLineInfo) {
                view.isDisplayLnPanel = showScrollLineInfo
                lastAppliedShowScrollLineInfo = showScrollLineInfo
            }
            if (lastAppliedFastMode != isFastMode) {
                view.isCursorAnimationEnabled = !isFastMode
                lastAppliedFastMode = isFastMode
            }
            if (lastAppliedInitialPreviewLines != initialPreviewLines) {
                view.setInitialPreviewLines(initialPreviewLines)
                lastAppliedInitialPreviewLines = initialPreviewLines
            }
            if (lastAppliedLineNumberAlign != lineNumberAlign) {
                view.setLineNumberPaintAlign(if (lineNumberAlign == "right") android.graphics.Paint.Align.RIGHT else if (lineNumberAlign == "center") android.graphics.Paint.Align.CENTER else android.graphics.Paint.Align.LEFT)
                lastAppliedLineNumberAlign = lineNumberAlign
            }
            if (lastAppliedIsLineNumberRightOfDivider != isLineNumberRightOfDivider) {
                view.setLineNumberRightOfDivider(isLineNumberRightOfDivider)
                lastAppliedIsLineNumberRightOfDivider = isLineNumberRightOfDivider
            }
            if (lastAppliedIsLineNumberPinned != isLineNumberPinned) {
                view.setPinLineNumber(isLineNumberPinned)
                lastAppliedIsLineNumberPinned = isLineNumberPinned
            }
            if (lastAppliedLineNumberColor != lineNumberColor) {
                try { view.colorScheme.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.parseColor(lineNumberColor)) } catch(e: Exception) {}
                lastAppliedLineNumberColor = lineNumberColor
            }
            if (lastAppliedLineDividerColor != lineDividerColor) {
                try { view.colorScheme.setColor(EditorColorScheme.LINE_DIVIDER, android.graphics.Color.parseColor(lineDividerColor)) } catch(e: Exception) {}
                lastAppliedLineDividerColor = lineDividerColor
            }
            
            try {
                if (editorTextColor != "auto" && editorTextColor.isNotEmpty()) {
                    view.colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.parseColor(editorTextColor))
                }
            } catch (e: Exception) {}

            if (lastAppliedTextActionMenuItems != textActionMenuItems) {
                view.textActionMenuOrder = textActionMenuItems
                lastAppliedTextActionMenuItems = textActionMenuItems
            }
            if (lastAppliedTextActionMenuHidden != textActionMenuHidden) {
                view.textActionMenuHidden = textActionMenuHidden
                lastAppliedTextActionMenuHidden = textActionMenuHidden
            }
            if (lastAppliedTextActionMenuBgColor != textActionMenuBgColor) {
                if (textActionMenuBgColor != "auto") {
                    try {
                        view.colorScheme.setColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND, android.graphics.Color.parseColor(textActionMenuBgColor))
                    } catch (e: Exception) {}
                }
                lastAppliedTextActionMenuBgColor = textActionMenuBgColor
            }
            
            // Update font family
            if (lastAppliedFontFamily != fontFamily) {
                val assets = view.context.assets
                when (fontFamily) {
                    "JetBrains Mono" -> view.setTypefaceText(Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf"))
                    "Ubuntu" -> view.setTypefaceText(Typeface.createFromAsset(assets, "Ubuntu-Regular.ttf"))
                    "Roboto" -> view.setTypefaceText(Typeface.createFromAsset(assets, "Roboto-Regular.ttf"))
                    else -> view.setTypefaceText(Typeface.MONOSPACE)
                }
                lastAppliedFontFamily = fontFamily
            }

            // Only update text if it strictly differs.
            if (view.text.toString() != content) {
                isSettingTextProgrammatically.value = true
                view.setText(content)
                isSettingTextProgrammatically.value = false
            }
            
            // REMOVED: These methods trigger expensive layout recalculation on every update
            // They should only be set in factory or when settings actually change
            // view.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            // view.setWrapLineSpacing(wrapLineSpacingExtra, wrapLineSpacingMultiplier)
            // view.setDividerMargin(0f, horizontalPadding * dp)
            // view.setExtraMarginRight(horizontalPadding * dp)
            // view.setLineNumberMarginLeft(horizontalPadding * dp)

            try {
                val sColor = android.graphics.Color.parseColor(scrollbarColor)
                if (view.colorScheme.getColor(EditorColorScheme.SCROLL_BAR_THUMB) != sColor) {
                    view.colorScheme?.setColor(EditorColorScheme.SCROLL_BAR_THUMB, sColor)
                    view.colorScheme?.setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, sColor)
                }
            } catch (e: Exception) {}
            
            try {
                val color = android.graphics.Color.parseColor(backgroundColor)
                if (view.colorScheme?.getColor(EditorColorScheme.WHOLE_BACKGROUND) != color) {
                    val r = android.graphics.Color.red(color)
                    val g = android.graphics.Color.green(color)
                    val b = android.graphics.Color.blue(color)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                    
                    view.colorScheme?.setColor(EditorColorScheme.WHOLE_BACKGROUND, color)
                    view.colorScheme?.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, color)
                    
                    if (luminance < 0.5) {
                        view.colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.WHITE)
                        view.colorScheme?.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.GRAY)
                    } else {
                        view.colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.BLACK)
                        view.colorScheme?.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.DKGRAY)
                    }
                }

                val searchMatchColor = try { android.graphics.Color.parseColor(searchMatchBackgroundColor) } catch (e: Exception) { 0xffffff00.toInt() }
                if (view.colorScheme?.getColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND) != searchMatchColor) {
                    view.colorScheme?.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, searchMatchColor)
                }

                val currentLineColor = try { android.graphics.Color.parseColor(currentLineBackgroundColor) } catch (e: Exception) { 0x10000000 }
                if (view.colorScheme?.getColor(EditorColorScheme.CURRENT_LINE) != currentLineColor) {
                    view.colorScheme?.setColor(EditorColorScheme.CURRENT_LINE, currentLineColor)
                }
                
                // Re-apply text color if needed after background update (as background update might reset scheme parts or we want to ensure precedence)
                // Actually the background update block above re-runs the luminance logic which might reset TEXT_NORMAL.
                // So we need to re-apply editorTextColor if it's not auto.
                if (editorTextColor != "auto" && editorTextColor.isNotEmpty()) {
                    try {
                        view.colorScheme?.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.parseColor(editorTextColor))
                    } catch (e: Exception) {}
                }

                val cColor = try { android.graphics.Color.parseColor(cursorColor) } catch (e: Exception) { 0xFF000000.toInt() }
                if (view.colorScheme?.getColor(EditorColorScheme.SELECTION_INSERT) != cColor) {
                    view.colorScheme?.setColor(EditorColorScheme.SELECTION_INSERT, cColor)
                }

                val hColor = try { android.graphics.Color.parseColor(handleColor) } catch (e: Exception) { 0xFF000000.toInt() }
                if (view.colorScheme?.getColor(EditorColorScheme.SELECTION_HANDLE) != hColor) {
                    view.colorScheme?.setColor(EditorColorScheme.SELECTION_HANDLE, hColor)
                }

                if (lastAppliedHandleStyle != handleStyle) {
                    view.setSelectionHandleStyle(when (handleStyle) {
                        "drop" -> HandleStyleDrop(view.context)
                        "none" -> HandleStyleNone()
                        else -> HandleStyleSideDrop(view.context)
                    })
                    lastAppliedHandleStyle = handleStyle
                }

                if (scrollbarStyle == "rounded") {
                    val sColor = try { android.graphics.Color.parseColor(scrollbarColor) } catch(e: Exception) { 0xFFA0888888.toInt() }
                    val drawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 100f
                        setColor(sColor)
                    }
                    view.renderer?.setVerticalScrollbarThumbDrawable(drawable)
                    view.renderer?.setHorizontalScrollbarThumbDrawable(drawable)
                } else {
                    view.renderer?.setVerticalScrollbarThumbDrawable(null)
                    view.renderer?.setHorizontalScrollbarThumbDrawable(null)
                }
            } catch (e: Exception) {}
        },
        modifier = modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val editorControl = remember { EditorControl() }
    val localContext = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadSettings(localContext)
    }
    
    LaunchedEffect(uiState.isReadOnly, uiState.showToolbar) {
        if (uiState.isReadOnly && uiState.showToolbar) {
            kotlinx.coroutines.delay(2000)
            viewModel.setShowToolbar(false)
        }
    }
    
    val handleBack = {
        if (uiState.showSettings) {
            viewModel.setShowSettings(false)
        } else if (!uiState.autoSave && uiState.isModified) {
            viewModel.setShowExitConfirmation(true)
        } else {
            onBack()
        }
    }

    androidx.activity.compose.BackHandler(onBack = handleBack)
    
    if (uiState.showSettings) {
        EditorSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { viewModel.setShowSettings(false) },
            onFontSizeChange = { viewModel.setFontSize(localContext, it) },
            onToggleLineNumbers = { viewModel.toggleLineNumbers(localContext) },
            onToggleWordWrap = { viewModel.toggleWordWrap(localContext) },
            onBackgroundColorChange = { viewModel.setBackgroundColor(localContext, it) }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = uiState.showToolbar || !uiState.isReadOnly,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    TopAppBar(
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = try { Color(android.graphics.Color.parseColor(uiState.uiColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface }
                    ),
                    navigationIcon = {
                            IconButton(onClick = { 
                                val pos = editorControl.getCurrentCursorPosition()
                                viewModel.setCursorPosition(pos, uiState.cursorLine - 1, uiState.cursorColumn)
                                viewModel.setShowToc(true) 
                            }) {
                                Icon(Icons.Default.Menu, "目录")
                            }
                        },
                        actions = {
                        IconButton(onClick = { viewModel.saveFile(localContext) }) {
                            Icon(Icons.Default.Save, "保存")
                        }
                        IconButton(onClick = { editorControl.undo() }, enabled = editorControl.canUndo()) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "撤销")
                        }
                        IconButton(onClick = { editorControl.redo() }, enabled = editorControl.canRedo()) {
                            Icon(Icons.AutoMirrored.Filled.Redo, "反撤销")
                        }
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                        
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(try { Color(android.graphics.Color.parseColor(uiState.menuColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface })
                        ) {
                            DropdownMenuItem(
                                text = { Text("返回") },
                                onClick = { showMoreMenu = false; handleBack() },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("搜索") },
                                onClick = { 
                                    viewModel.setShowSearch(!uiState.showSearch)
                                    showMoreMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.Search, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                onClick = { viewModel.setShowRenameDialog(true); showMoreMenu = false },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("属性") },
                                onClick = { viewModel.setShowFileProperties(true); showMoreMenu = false },
                                leadingIcon = { Icon(Icons.Default.Info, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("重做 (还原为初始)") },
                                onClick = { 
                                    viewModel.updateContent(localContext, uiState.originalContent)
                                    showMoreMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, null) }
                            )
                            DropdownMenuItem(
                                    text = { Text("只读模式: ${if (uiState.isReadOnly) "ON" else "OFF"}") },
                                    onClick = { viewModel.toggleReadOnly(); showMoreMenu = false },
                                    leadingIcon = { Icon(if(uiState.isReadOnly) Icons.Default.Lock else Icons.Default.LockOpen, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("编辑器设置") },
                                    onClick = { viewModel.setShowSettings(true); showMoreMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )
                            }
                        }
                    )
                }
                
                if (uiState.showSearch) {
                    SearchPanel(
                        searchQuery = uiState.searchQuery,
                        replaceText = uiState.replaceText,
                        currentMatch = uiState.currentMatch,
                        totalMatches = uiState.totalMatches,
                        searchAsRegExp = uiState.searchAsRegExp,
                        searchWholeWord = uiState.searchWholeWord,
                        searchMatchCase = uiState.searchMatchCase,
                        backgroundColor = uiState.searchColor,
                        onSearchQueryChange = { 
                            viewModel.setSearchQuery(it)
                            if (it.isNotEmpty()) {
                                editorControl.search(it, uiState.searchAsRegExp, uiState.searchMatchCase, uiState.searchWholeWord)
                            } else {
                                editorControl.stopSearch()
                            }
                        },
                        onReplaceTextChange = { viewModel.setReplaceText(it) },
                        onToggleRegExp = { 
                            viewModel.setSearchAsRegExp(localContext, it)
                            if (uiState.searchQuery.isNotEmpty()) {
                                editorControl.search(uiState.searchQuery, it, uiState.searchMatchCase, uiState.searchWholeWord)
                            }
                        },
                        onToggleWholeWord = { 
                            viewModel.setSearchWholeWord(localContext, it)
                            if (uiState.searchQuery.isNotEmpty()) {
                                editorControl.search(uiState.searchQuery, uiState.searchAsRegExp, uiState.searchMatchCase, it)
                            }
                        },
                        onToggleMatchCase = { 
                            viewModel.setSearchMatchCase(localContext, it)
                            if (uiState.searchQuery.isNotEmpty()) {
                                editorControl.search(uiState.searchQuery, uiState.searchAsRegExp, it, uiState.searchWholeWord)
                            }
                        },
                        onFindNext = { editorControl.findNext() },
                        onFindPrevious = { editorControl.findPrevious() },
                        onReplace = { editorControl.replace(uiState.replaceText) },
                        onReplaceAll = { editorControl.replaceAll(uiState.replaceText) },
                        onClose = { viewModel.setShowSearch(false) }
                    )
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    SoraEditorView(
                        content = uiState.content,
                        onContentChange = { viewModel.updateContent(localContext, it) },
                        onSelectionChange = { pos, line, col -> viewModel.setCursorPosition(pos, line, col) },
                        fontSize = uiState.fontSize,
                        showLineNumbers = uiState.showLineNumbers,
                        wordWrap = uiState.wordWrap,
                        isLineNumberPinned = uiState.isLineNumberPinned,
                        editable = !uiState.isReadOnly,
                        backgroundColor = uiState.backgroundColor,
                        searchMatchBackgroundColor = uiState.searchMatchBackgroundColor,
                        control = editorControl,
                        onSearchMatchesChange = { current, total -> viewModel.setMatchResults(current, total) },
                        onScroll = { if (uiState.isReadOnly && uiState.showToolbar) viewModel.setShowToolbar(false) },
                        onTap = { if (uiState.isReadOnly) viewModel.toggleToolbar() },
                        autoFocus = uiState.shouldAutoFocus,
                        lineSpacingMultiplier = uiState.lineSpacingMultiplier,
                        lineSpacingExtra = uiState.lineSpacingExtra,
                        wrapLineSpacingMultiplier = uiState.wrapLineSpacingMultiplier,
                        wrapLineSpacingExtra = uiState.wrapLineSpacingExtra,
                        horizontalPadding = uiState.horizontalPadding,
                        highlightCurrentLine = uiState.highlightCurrentLine,
                        currentLineBackgroundColor = uiState.currentLineBackgroundColor,
                        cursorColor = uiState.cursorColor,
                        handleColor = uiState.handleColor,
                        cursorWidth = uiState.cursorWidth,
                        handleStyle = uiState.handleStyle,
                        fontFamily = uiState.fontFamily,
                        scrollbarColor = uiState.scrollbarColor,
                        showScrollLineInfo = uiState.showScrollLineInfo,
                        scrollbarStyle = uiState.scrollbarStyle,
                        isFastMode = uiState.isFastMode,
                        initialPreviewLines = uiState.initialPreviewLines,
                        lineNumberAlign = uiState.lineNumberAlign,
                        isLineNumberRightOfDivider = uiState.isLineNumberRightOfDivider,
                        lineNumberColor = uiState.lineNumberColor,
                        lineDividerColor = uiState.lineDividerColor,
                        editorTextColor = uiState.editorTextColor,
                        textActionMenuItems = uiState.textActionMenuItems,
                        textActionMenuHidden = uiState.textActionMenuHidden,
                        textActionMenuBgColor = uiState.textActionMenuBgColor
                    )
                }
                
                Column {
                    if (uiState.showSymbolBar && !uiState.isReadOnly) {
                        SymbolBar(
                            uiColor = uiState.symbolBarColor,
                            textColor = uiState.symbolTextColor,
                            style = uiState.symbolBarStyle,
                            onSymbolClick = { editorControl.insertText(it) }
                        )
                    }
                    if (uiState.showStatusBar) {
                        StatusBar(
                            uiColor = uiState.uiColor,
                            fileName = uiState.fileName,
                            cursorLine = uiState.cursorLine,
                            cursorColumn = uiState.cursorColumn,
                            currentCursorPos = uiState.currentCursorPos
                        )
                    }
                }
            }
            
            if (uiState.showToc) {
                TocPanel(
                    content = uiState.content,
                    currentCursorPos = uiState.currentCursorPos,
                    tocMode = uiState.tocMode,
                    surfaceColor = uiState.tocColor,
                    onModeChange = { viewModel.setTocMode(it) },
                    onChapterClick = { editorControl.jumpTo(it); viewModel.setShowToc(false) },
                    onDismiss = { viewModel.setShowToc(false) },
                    scrollbarColor = uiState.scrollbarColor
                )
            }
            
            if (uiState.showRenameDialog) {
                RenameDialog(
                    currentName = uiState.fileName,
                    backgroundColor = uiState.menuColor,
                    onRename = { viewModel.renameFile(it); viewModel.setShowRenameDialog(false) },
                    onDismiss = { viewModel.setShowRenameDialog(false) }
                )
            }

            if (uiState.showFileProperties) {
                FilePropertiesDialog(
                    properties = viewModel.getFileDetails(),
                    backgroundColor = uiState.menuColor,
                    onDismiss = { viewModel.setShowFileProperties(false) }
                )
            }

            if (uiState.showExitConfirmation) {
                ExitConfirmationDialog(
                    onSave = { viewModel.saveFile(localContext); onBack() },
                    onDiscard = { onBack() },
                    onDismiss = { viewModel.setShowExitConfirmation(false) }
                )
            }
        }
    }
}

@Composable
fun SymbolBar(uiColor: String, textColor: String, style: String, onSymbolClick: (String) -> Unit) {
    val symbols = listOf(",", ".", ";", "(", ")", "{", "}", "[", "]", "\"", "'", ":", "/", "<", ">", "=", "+", "-", "*", "&", "|", "!", "?", "#", "@", "$", "%", "^", "~", "`")
    val bgColor = try { Color(android.graphics.Color.parseColor(uiColor)) } catch(e:Exception) { Color(0xFFF0F0F0) }
    val tColor = try { Color(android.graphics.Color.parseColor(textColor)) } catch(e:Exception) { Color.Black }
    
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bgColor)
            .border(androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))),
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (style == "flat") 0.dp else 4.dp)
    ) {
        items(symbols.size) { index ->
            val content = @Composable {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(symbols[index], fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = tColor)
                }
            }
            
            if (style == "flat") {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 36.dp)
                        .clickable { onSymbolClick(symbols[index]) }
                ) {
                    content()
                }
            } else {
                Surface(
                    onClick = { onSymbolClick(symbols[index]) },
                    modifier = Modifier
                        .size(width = 36.dp, height = 36.dp),
                    shape = if (style == "rounded") RoundedCornerShape(4.dp) else androidx.compose.ui.graphics.RectangleShape,
                    color = Color.White.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun StatusBar(uiColor: String, fileName: String, cursorLine: Int, cursorColumn: Int, currentCursorPos: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        color = try { Color(android.graphics.Color.parseColor(uiColor)) } catch(e:Exception) { Color(0xFFEEEEEE) },
        tonalElevation = 2.dp,
        border = BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
             Text(
                text = fileName,
                fontSize = 11.sp,
                color = Color.DarkGray,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "第 ${cursorLine} 行, 第 ${cursorColumn} 列, 第 ${currentCursorPos} 字",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun SearchPanel(
    searchQuery: String,
    replaceText: String,
    currentMatch: Int,
    totalMatches: Int,
    searchAsRegExp: Boolean,
    searchWholeWord: Boolean,
    searchMatchCase: Boolean,
    backgroundColor: String,
    onSearchQueryChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onToggleRegExp: (Boolean) -> Unit,
    onToggleWholeWord: (Boolean) -> Unit,
    onToggleMatchCase: (Boolean) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = try { Color(android.graphics.Color.parseColor(backgroundColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface }, tonalElevation = 1.dp) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("查找文本", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (totalMatches > 0) {
                                Text("${currentMatch.coerceAtLeast(0)}/$totalMatches", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                            }
                            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
                Button(onClick = onFindPrevious, contentPadding = PaddingValues(0.dp), modifier = Modifier.defaultMinSize(minWidth = 48.dp)) { Text("上个", fontSize = 12.sp) }
                Button(onClick = onFindNext, contentPadding = PaddingValues(0.dp), modifier = Modifier.defaultMinSize(minWidth = 48.dp)) { Text("下个", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = onReplaceTextChange,
                    placeholder = { Text("替换到的文本", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                TextButton(onClick = onReplace) { Text("替换", fontSize = 13.sp) }
                TextButton(onClick = onReplaceAll, contentPadding = PaddingValues(horizontal = 4.dp)) { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("全部", fontSize = 11.sp)
                        Text("替换", fontSize = 11.sp)
                    }
                }
                
                var showOptions by remember { mutableStateOf(false) }
                val backgroundColorParsed = try { Color(android.graphics.Color.parseColor(backgroundColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface }
                Box {
                    TextButton(onClick = { showOptions = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("选项", fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false },
                        modifier = Modifier.background(backgroundColorParsed)
                    ) {
                        DropdownMenuItem(
                            text = { Text("区分大小写 (Ab)") },
                            onClick = { onToggleMatchCase(!searchMatchCase) },
                            leadingIcon = { Icon(if(searchMatchCase) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if(searchMatchCase) MaterialTheme.colorScheme.primary else Color.Gray) }
                        )
                        DropdownMenuItem(
                            text = { Text("全词匹配 (W)") },
                            onClick = { onToggleWholeWord(!searchWholeWord) },
                            leadingIcon = { Icon(if(searchWholeWord) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if(searchWholeWord) MaterialTheme.colorScheme.primary else Color.Gray) }
                        )
                        DropdownMenuItem(
                            text = { Text("正则表达式 (.*)") },
                            onClick = { onToggleRegExp(!searchAsRegExp) },
                            leadingIcon = { Icon(if(searchAsRegExp) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if(searchAsRegExp) MaterialTheme.colorScheme.primary else Color.Gray) }
                        )
                    }
                }
            }
        }
    }
}

data class Chapter(val index: Int, val pos: Int, val title: String)

@Composable
fun TocPanel(
    content: String,
    currentCursorPos: Int,
    tocMode: String,
    surfaceColor: String,
    onModeChange: (String) -> Unit,
    onChapterClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    scrollbarColor: String = "#A0888888"
) {
    val chapters = remember(content, tocMode) {
        if (tocMode == "chars") {
            val count = kotlin.math.ceil(content.length / 2000.0).toInt()
            List(count) { i -> Chapter(i, i * 2000, "第 ${i + 1} 章") }
        } else {
            val result = mutableListOf<Chapter>()
            var currentPos = 0
            var lineCount = 0
            var chunkStartLine = 1
            for (i in content.indices) {
                if (content[i] == '\n') {
                    lineCount++
                    if (lineCount % 100 == 0) {
                        result.add(Chapter(result.size, currentPos, "第 $chunkStartLine - $lineCount 行"))
                        chunkStartLine = lineCount + 1
                    }
                }
                currentPos++
            }
            if (result.isEmpty() || lineCount >= chunkStartLine) {
                 result.add(Chapter(result.size, if(result.isEmpty()) 0 else currentPos, "第 $chunkStartLine - ${lineCount + 1} 行"))
            }
            result
        }
    }
    
    val activeIndex = chapters.indexOfLast { it.pos <= currentCursorPos }.coerceAtLeast(0)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(activeIndex) {
        if (activeIndex > 0) listState.scrollToItem((activeIndex - 5).coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(onClick = onDismiss)) {
        Surface(Modifier.fillMaxHeight().fillMaxWidth(0.75f).clickable(enabled = false) { }, color = try { Color(android.graphics.Color.parseColor(surfaceColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface }, tonalElevation = 8.dp) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("目录", style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("chars" to "按字", "lines" to "按行").forEach { (mode, label) ->
                            Button(
                                onClick = { onModeChange(mode) }, 
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tocMode == mode) Color(0xFFE0E0E0) else Color.Transparent,
                                    contentColor = if (tocMode == mode) Color.Black else Color.DarkGray
                                ), 
                                modifier = Modifier.height(32.dp), 
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
                HorizontalDivider()
                Box(modifier = Modifier.weight(1f)) {
                    androidx.compose.foundation.lazy.LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(chapters.size) { index ->
                            val isActive = index == activeIndex
                            Surface(
                                onClick = { onChapterClick(chapters[index].pos); onDismiss() },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                            ) {
                                Text(chapters[index].title, modifier = Modifier.padding(20.dp, 12.dp), fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else null)
                            }
                        }
                    }
                    
                    SoraStyleScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        lazyListState = listState,
                        totalItems = chapters.size,
                        scrollbarColor = scrollbarColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorSettingsScreen(
    uiState: EditorUiState,
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onToggleLineNumbers: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onBackgroundColorChange: (String) -> Unit
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑器设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    color = try { Color(android.graphics.Color.parseColor(uiState.backgroundColor)) } catch(e:Exception) { Color.Gray },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("预览文本效果 Preview Text", fontSize = uiState.fontSize.sp, color = if (uiState.backgroundColor == "#000000") Color.White else Color.Black)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("字体大小: ${uiState.fontSize.toInt()}px")
                    Slider(value = uiState.fontSize, onValueChange = onFontSizeChange, valueRange = 12f..36f)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("行间距倍率: ${"%.1f".format(uiState.lineSpacingMultiplier)}")
                    Slider(value = uiState.lineSpacingMultiplier, onValueChange = { viewModel.setLineSpacingMultiplier(localContext, it) }, valueRange = 0.5f..3.0f)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("自动换行行内倍率 (逻辑行内): ${"%.1f".format(uiState.wrapLineSpacingMultiplier)}")
                    Slider(value = uiState.wrapLineSpacingMultiplier, onValueChange = { viewModel.setWrapLineSpacing(localContext, uiState.wrapLineSpacingExtra, it) }, valueRange = 0.5f..3.0f)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("左右页边距: ${uiState.horizontalPadding.toInt()}dp")
                    Slider(value = uiState.horizontalPadding, onValueChange = { viewModel.setHorizontalPadding(localContext, it) }, valueRange = 0f..50f)
                }

                SettingsSwitchItem("显示行号", "在左侧显示行号", uiState.showLineNumbers) { viewModel.toggleLineNumbers(localContext) }
                SettingsSwitchItem("固定行号", "行号固定不随行移动 (Sticky)", uiState.isLineNumberPinned) { viewModel.toggleLineNumberPinned(localContext) }
                SettingsSwitchItem("自动换行", "自动折行显示", uiState.wordWrap) { viewModel.toggleWordWrap(localContext) }
                SettingsSwitchItem("高亮当前行", "突出显示光标所在的行", uiState.highlightCurrentLine) { viewModel.setHighlightCurrentLine(localContext, it) }
                SettingsSwitchItem("行号位于竖线右侧", "将行号显示在分隔线右侧（靠近代码）", uiState.isLineNumberRightOfDivider) { viewModel.setLineNumberRightOfDivider(localContext, it) }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("行号文本对齐", fontSize = 12.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        mapOf("left" to "左对齐", "center" to "居中", "right" to "右对齐").forEach { (id, label) ->
                            Button(
                                onClick = { viewModel.setLineNumberAlign(localContext, id) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (uiState.lineNumberAlign == id) MaterialTheme.colorScheme.primary else Color.LightGray)
                            ) {
                                Text(label, fontSize = 10.sp)
                            }
                        }
                    }
                }
                
                SettingsSwitchItem("自动保存", "编辑时自动保存", uiState.autoSave) { viewModel.setAutoSave(localContext, it) }
                SettingsSwitchItem("极速模式", "禁用动画（如光标移动）以获得更快的响应", uiState.isFastMode) { viewModel.setFastMode(localContext, it) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("初始加载行数 (同步): ${uiState.initialPreviewLines}", modifier = Modifier.weight(1f))
                        Text("越多加载越慢", color = Color.Red, fontSize = 10.sp)
                    }
                    Slider(
                        value = uiState.initialPreviewLines.toFloat(),
                        onValueChange = { viewModel.setInitialPreviewLines(localContext, it.toInt()) },
                        valueRange = 0f..200f,
                    )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("字体样式", style = MaterialTheme.typography.titleMedium)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Monospace", "JetBrains Mono", "Ubuntu", "Roboto").forEach { font ->
                            val isSelected = uiState.fontFamily == font
                            Button(
                                onClick = { viewModel.setFontFamily(localContext, font) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    contentColor = if (isSelected) Color.White else Color.Black
                                )
                            ) {
                                Text(font, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                SettingsSwitchItem("显示状态栏", "显示底部的行、列、字符数信息", uiState.showStatusBar) { viewModel.toggleStatusBar(localContext) }
                SettingsSwitchItem("符号快捷键", "在底部显示常用符号栏", uiState.showSymbolBar) { viewModel.toggleSymbolBar(localContext) }
                SettingsSwitchItem("显示滚动详情 (行号)", "拖动滚动条时显示当前行号", uiState.showScrollLineInfo) { viewModel.setShowScrollLineInfo(localContext, it) }
                SettingsSwitchItem("键盘开启时调整窗口大小", "启用 adjustResize，避免输入法遮挡光标", uiState.keyboardAdjust) { viewModel.setKeyboardAdjust(localContext, it) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("搜索首选项", style = MaterialTheme.typography.titleMedium)
                    SettingsSwitchItem("正则表达式", "默认启用正则匹配", uiState.searchAsRegExp) { viewModel.setSearchAsRegExp(localContext, it) }
                    SettingsSwitchItem("全词匹配", "默认启用全词匹配", uiState.searchWholeWord) { viewModel.setSearchWholeWord(localContext, it) }
                    SettingsSwitchItem("区分大小写", "默认区分大小写", uiState.searchMatchCase) { viewModel.setSearchMatchCase(localContext, it) }
                }

                EditorColorSettings(uiState, viewModel, localContext)
     Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("滚动条样式", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            mapOf("default" to "Sora (直角)", "rounded" to "Chrome (圆角)").forEach { (id, label) ->
                                Button(
                                    onClick = { viewModel.setScrollbarStyle(localContext, id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (uiState.scrollbarStyle == id) MaterialTheme.colorScheme.primary else Color.LightGray)
                                ) {
                                    Text(label, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("光标宽度: ${"%.1f".format(uiState.cursorWidth)}px")
                        Slider(value = uiState.cursorWidth, onValueChange = { viewModel.setCursorWidth(localContext, it) }, valueRange = 1f..10f)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("光标提手样式", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            mapOf("side_drop" to "侧水滴", "drop" to "正水滴", "none" to "无提手").forEach { (id, label) ->
                                Button(
                                    onClick = { viewModel.setHandleStyle(localContext, id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (uiState.handleStyle == id) MaterialTheme.colorScheme.primary else Color.LightGray)
                                ) {
                                    Text(label, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("符号快捷键颜色 (Symbol Bar)", fontSize = 12.sp, color = Color.Gray)
                        val barColors = listOf("#F5F5F5" to "灰", "#FFFFFF" to "白", "#EEEEEE" to "深灰", "#000000" to "黑", "#EFEBE9" to "米", "#E8EAF6" to "蓝灰")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            barColors.forEach { (c, l) -> 
                                ColorOption(c, l, uiState.symbolBarColor == c) { viewModel.setSymbolBarColor(localContext, c) }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("符号文本颜色 (Symbol Text)", fontSize = 12.sp, color = Color.Gray)
                        val textColors = listOf("#FF000000" to "黑", "#FF888888" to "灰", "#FFFFFFFF" to "白", "#FF0000FF" to "蓝", "#FFFF0000" to "红")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            textColors.forEach { (c, l) -> 
                                ColorOption(c, l, uiState.symbolTextColor == c) { viewModel.setSymbolTextColor(localContext, c) }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("符号快捷键样式", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            mapOf("rounded" to "圆角框", "flat" to "无框 (全屏)", "classic" to "直角框").forEach { (id, label) ->
                                Button(
                                    onClick = { viewModel.setSymbolBarStyle(localContext, id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (uiState.symbolBarStyle == id) MaterialTheme.colorScheme.primary else Color.LightGray)
                                ) {
                                    Text(label, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("编辑浮动菜单 (长按/双击弹出)", style = MaterialTheme.typography.titleMedium)
                    
                    val allItemLabels = mapOf(
                        "select_all" to "全选",
                        "copy" to "复制",
                        "paste" to "粘贴",
                        "cut" to "剪切",
                        "long_select" to "自由选择"
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uiState.textActionMenuItems.forEachIndexed { index, id ->
                            val label = allItemLabels[id] ?: id
                            val isHidden = id in uiState.textActionMenuHidden
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isHidden) Color.LightGray.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleTextActionMenuItemVisibility(localContext, id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isHidden) Color.Gray else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Text(
                                        text = label,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        color = if (isHidden) Color.Gray else Color.Unspecified
                                    )
                                    
                                    IconButton(
                                        onClick = { 
                                            if (index > 0) {
                                                val newList = uiState.textActionMenuItems.toMutableList()
                                                val item = newList.removeAt(index)
                                                newList.add(index - 1, item)
                                                viewModel.setTextActionMenuOrder(localContext, newList)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp), tint = if (index > 0) Color.Gray else Color.Transparent)
                                    }
                                    
                                    IconButton(
                                        onClick = { 
                                            if (index < uiState.textActionMenuItems.size - 1) {
                                                val newList = uiState.textActionMenuItems.toMutableList()
                                                val item = newList.removeAt(index)
                                                newList.add(index + 1, item)
                                                viewModel.setTextActionMenuOrder(localContext, newList)
                                            }
                                        },
                                        enabled = index < uiState.textActionMenuItems.size - 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp), tint = if (index < uiState.textActionMenuItems.size - 1) Color.Gray else Color.Transparent)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    Text("浮动菜单背景颜色", fontSize = 12.sp, color = Color.Gray)
                    val bgColors = listOf("auto" to "默认", "#FFFFFF" to "白", "#F5F5F5" to "灰", "#000000" to "黑", "#EFEBE9" to "米", "#E8EAF6" to "蓝灰")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bgColors.forEach { (c, l) -> 
                            ColorOption(c, l, uiState.textActionMenuBgColor == c) { viewModel.setTextActionMenuBgColor(localContext, c) }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("配置 JSON", style = MaterialTheme.typography.titleMedium)
                    var jsonText by remember { mutableStateOf(viewModel.getSettingsJson()) }
                    
                    LaunchedEffect(uiState) {
                        jsonText = viewModel.getSettingsJson()
                    }

                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.resetSettings(localContext) }) {
                            Text("重置所有设置", color = Color.Red)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                viewModel.applySettingsFromJson(localContext, jsonText)
                            }
                        ) {
                            Text("保存 JSON 设置")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            SoraStyleScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).padding(padding),
                scrollState = scrollState,
                scrollbarColor = uiState.scrollbarColor
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun RenameDialog(currentName: String, backgroundColor: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = try { Color(android.graphics.Color.parseColor(backgroundColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface },
        title = { Text("重命名") }, 
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("新名字") }) }, 
        confirmButton = { TextButton(onClick = { onRename(name) }) { Text("OK") } }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FilePropertiesDialog(properties: Map<String, String>, backgroundColor: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = try { Color(android.graphics.Color.parseColor(backgroundColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface },
        title = { Text("属性") }, 
        text = { Column { properties.forEach { (k, v) -> Text("$k: $v") } } }, 
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ExitConfirmationDialog(onSave: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("保存？") }, text = { Text("内容已修改") }, confirmButton = { TextButton(onClick = onSave) { Text("保存") } }, dismissButton = { Row { TextButton(onClick = onDiscard) { Text("不保存") }; TextButton(onClick = onDismiss) { Text("取消") } } })
}
@Composable
fun SoraStyleScrollbar(
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null,
    lazyListState: androidx.compose.foundation.lazy.LazyListState? = null,
    totalItems: Int = 0,
    scrollbarColor: String = "#A0888888",
    scrollbarStyle: String = "default"
) {
    val color = try { Color(android.graphics.Color.parseColor(scrollbarColor)) } catch(e:Exception) { Color.Gray.copy(alpha = 0.5f) }
    
    val isScrollingProp = remember(scrollState?.isScrollInProgress, lazyListState?.isScrollInProgress) {
        (scrollState?.isScrollInProgress ?: false) || (lazyListState?.isScrollInProgress ?: false)
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isScrollingProp) {
        if (isScrollingProp) {
            visible = true
        } else {
            kotlinx.coroutines.delay(2000)
            visible = false
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxHeight().width(30.dp)) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            
            val (thumbHeightPercent, scrollPercent) = when {
                scrollState != null -> {
                    val viewportHeight = trackHeightPx
                    val totalHeight = (scrollState.maxValue.toFloat() + viewportHeight).coerceAtLeast(1f)
                    (viewportHeight / totalHeight).coerceIn(0.1f, 1f) to 
                        if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue.toFloat() else 0f
                }
                lazyListState != null && totalItems > 0 -> {
                    val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                    val visibleCount = visibleItems.size
                    (visibleCount.toFloat() / totalItems.toFloat()).coerceIn(0.1f, 1f) to
                        if (totalItems > visibleCount) lazyListState.firstVisibleItemIndex.toFloat() / (totalItems - visibleCount).coerceAtLeast(1).toFloat() else 0f
                }
                else -> 0.1f to 0f
            }

            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalItems, scrollState?.maxValue) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val thumbHeight = trackHeightPx * thumbHeightPercent
                                val travelDistance = (trackHeightPx - thumbHeight).coerceAtLeast(1f)
                                val deltaPercent = dragAmount.y / travelDistance
                                
                                when {
                                    scrollState != null -> {
                                        val newScroll = (scrollState.value + deltaPercent * scrollState.maxValue).coerceIn(0f, scrollState.maxValue.toFloat())
                                        scope.launch { scrollState.scrollTo(newScroll.toInt()) }
                                    }
                                    lazyListState != null && totalItems > 0 -> {
                                        val visibleCount = lazyListState.layoutInfo.visibleItemsInfo.size
                                        val maxIndex = (totalItems - visibleCount).coerceAtLeast(0)
                                        val currentPercent = lazyListState.firstVisibleItemIndex.toFloat() / maxIndex.coerceAtLeast(1).toFloat()
                                        val newPercent = (currentPercent + deltaPercent).coerceIn(0f, 1f)
                                        scope.launch { lazyListState.scrollToItem((newPercent * maxIndex).toInt()) }
                                    }
                                }
                            }
                        )
                    }
            )

            // Sora Style Thumb (Matches EditorRenderer.drawScrollBarVertical)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(10.dp)
                    .fillMaxHeight(thumbHeightPercent)
                    .graphicsLayer {
                        translationY = trackHeightPx * (1f - thumbHeightPercent) * scrollPercent
                    }
                    .background(
                        color = color,
                        shape = if (scrollbarStyle == "rounded") RoundedCornerShape(5.dp) else androidx.compose.ui.graphics.RectangleShape
                    )
            )
        }
    }
}
