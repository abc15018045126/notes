package com.abc15018045126.capacitor.soraeditor.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import org.json.JSONObject

data class EditorUiState(
    val content: String = "",
    val filePath: String = "",
    val fileName: String = "",
    val isModified: Boolean = false,
    val fontSize: Float = 18f,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val isReadOnly: Boolean = false,
    val showToolbar: Boolean = true,
    val showSearch: Boolean = false,
    val searchQuery: String = "",
    val replaceText: String = "",
    val currentMatch: Int = 0,
    val totalMatches: Int = 0,
    val showToc: Boolean = false,
    val tocMode: String = "chars", // "chars" or "lines"
    val showSettings: Boolean = false,
    val backgroundColor: String = "#FFFFFF",
    val isDarkTheme: Boolean = false,
    val currentCursorPos: Int = 0,
    val autoSave: Boolean = true,
    val showFileProperties: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showExitConfirmation: Boolean = false,
    val cursorLine: Int = 1,
    val cursorColumn: Int = 0,
    val originalContent: String = "",
    val showStatusBar: Boolean = true,
    val showSymbolBar: Boolean = true,
    val uiColor: String = "#F5F5F5",
    val tocColor: String = "#FFFFFF",
    val searchColor: String = "#F5F5F5",
    val menuColor: String = "#FFFFFF",
    val searchMatchBackgroundColor: String = "#FFF59D",
    val shouldAutoFocus: Boolean = false,
    val lineSpacingMultiplier: Float = 1.0f,
    val lineSpacingExtra: Float = 0f,
    val wrapLineSpacingMultiplier: Float = 1.0f,
    val wrapLineSpacingExtra: Float = 0f,
    val horizontalPadding: Float = 12f,
    val highlightCurrentLine: Boolean = true,
    val currentLineBackgroundColor: String = "#10000000",
    val cursorColor: String = "#FF000000",
    val handleColor: String = "#FF000000",
    val cursorWidth: Float = 2.0f,
    val handleStyle: String = "side_drop", // "drop", "side_drop", "none"
    val searchAsRegExp: Boolean = false,
    val searchWholeWord: Boolean = false,
    val searchMatchCase: Boolean = false,
    val fontFamily: String = "Monospace",
    val scrollbarColor: String = "#A0888888",
    val showScrollLineInfo: Boolean = true,
    val scrollbarStyle: String = "default", // "default" or "rounded"
    val keyboardAdjust: Boolean = true,
    val symbolBarColor: String = "#F5F5F5",
    val symbolTextColor: String = "#FF000000",
    val symbolBarStyle: String = "rounded" // "rounded", "flat"
)

class EditorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // ... existing loadFile method ...

    fun setCursorPosition(pos: Int, line: Int, col: Int) {
        _uiState.update { it.copy(
            currentCursorPos = pos,
            cursorLine = line + 1, // 1-indexed for display
            cursorColumn = col
        ) }
    }

    fun loadFile(context: Context, filePath: String, autoFocus: Boolean = false) {
        loadSettings(context)
        viewModelScope.launch {
            try {
                // Convert URI to actual path if needed
                var actualPath = if (filePath.startsWith("file://")) {
                    filePath.substring(7) // Remove "file://" prefix
                } else {
                    filePath
                }
                
                // Decode URI-encoded characters (like %E4%BD%9C%E8%80%85 for Chinese)
                actualPath = java.net.URLDecoder.decode(actualPath, "UTF-8")
                
                android.util.Log.d("EditorViewModel", "Loading file from: $actualPath")
                val file = File(actualPath)
                
                if (file.exists()) {
                    val content = file.readText()
                    android.util.Log.d("EditorViewModel", "File loaded successfully, size: ${content.length}")
                    _uiState.value = _uiState.value.copy(
                        content = content,
                        filePath = actualPath,
                        fileName = file.name,
                        originalContent = content,
                        isModified = false,
                        shouldAutoFocus = autoFocus
                    )
                } else {
                    android.util.Log.e("EditorViewModel", "File does not exist: $actualPath")
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error loading file", e)
                e.printStackTrace()
            }
        }
    }

    private var saveJob: kotlinx.coroutines.Job? = null

    fun updateContent(context: Context, newContent: String) {
        val normalizedNew = newContent.replace("\r\n", "\n")
        val normalizedCurrent = _uiState.value.content.replace("\r\n", "\n")
        
        // If content hasn't changed (ignoring line endings), don't update
        if (normalizedNew == normalizedCurrent && _uiState.value.content.isNotEmpty()) return
        
        // Check for trivial change (trailing whitespace/newline only) when not modified yet
        // This prevents updating the timestamp just because the editor added a newline
        val isTrivialChange = !_uiState.value.isModified && 
                              normalizedNew.trimEnd() == normalizedCurrent.trimEnd()

        if (isTrivialChange) {
            // Update content so state matches editor, but don't mark modified or save
            _uiState.update { it.copy(content = newContent) }
            return
        }
        
        _uiState.update { it.copy(
            content = newContent,
            isModified = true
        ) }
        
        if (_uiState.value.autoSave) {
            queueAutoSave(context)
        }
    }

    private fun queueAutoSave(context: Context) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Debounce 1s
            if (_uiState.value.isModified) {
                saveFile(context)
            }
        }
    }

    fun saveFile(context: Context): Boolean {
        return try {
            val contentToSave = _uiState.value.content
            val path = _uiState.value.filePath
            
            // Should ideally be IO, but for now we keep it simple or use runBlocking for exit safety? 
            // Better to just write. File writeText is blocking but OK for background thread.
            // But we are in ViewModel, often main thread call.
            // Let's protect against main thread blocking for large files?
            // But for saveOnExit (onPause) we need synchronous or strictly ordered.
            // For auto-save we are effectively inside a coroutine (from queueAutoSave).
            
            android.util.Log.d("EditorViewModel", "Saving file to: $path, content length: ${contentToSave.length}")
            
            val file = File(path)
            file.writeText(contentToSave)
            
            _uiState.update { state ->
                // Only clear isModified if the content we just saved is still the current content
                if (state.content == contentToSave) {
                    state.copy(isModified = false)
                } else {
                    state
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("EditorViewModel", "Failed to save file", e)
            e.printStackTrace()
            false
        }
    }

    fun saveOnExit(context: Context) {
        val state = _uiState.value
        // Cancel pending auto-save and save immediately if needed
        saveJob?.cancel()
        
        if (state.isModified || state.autoSave) {
            saveFile(context)
        }

        // Logic to rename based on first line, ONLY on exit
        val fileName = state.fileName
        // Heuristic: If filename is just numbers (timestamp) or starts with "Untitled", consider it "New"
        val isDefaultName = fileName.matches(Regex("^\\d+(\\.txt)?$")) || 
                           fileName.startsWith("Untitled") || 
                           fileName.startsWith("NewFile")

        if (isDefaultName) {
             val content = state.content
             val firstLine = content.lineSequence().firstOrNull()?.trim() ?: ""
             // Sanitize title: remove invalid chars, limit length
             val validTitle = firstLine.replace(Regex("[\\\\/:*?\"<>|]"), "").take(20).trim()
             
             if (validTitle.isNotEmpty() && validTitle != fileName.removeSuffix(".txt")) {
                 val newName = "$validTitle.txt"
                 android.util.Log.d("EditorViewModel", "Auto-renaming on exit: $fileName -> $newName")
                 renameFile(newName)
             }
        }
    }

    fun setFontSize(context: Context, size: Float) {
        _uiState.value = _uiState.value.copy(fontSize = size)
        saveSettings(context)
    }

    fun toggleLineNumbers(context: Context) {
        _uiState.update { it.copy(showLineNumbers = !it.showLineNumbers) }
        saveSettings(context)
    }

    fun toggleWordWrap(context: Context) {
        _uiState.update { it.copy(wordWrap = !it.wordWrap) }
        saveSettings(context)
    }

    fun toggleReadOnly() {
        _uiState.update { state ->
            val nextReadOnly = !state.isReadOnly
            state.copy(
                isReadOnly = nextReadOnly,
                showToolbar = if (nextReadOnly) false else true
            )
        }
    }

    fun toggleToolbar() {
        _uiState.update { it.copy(showToolbar = !it.showToolbar) }
    }

    fun setShowToolbar(show: Boolean) {
        _uiState.update { it.copy(showToolbar = show) }
    }

    fun setShowSearch(show: Boolean) {
        _uiState.update { it.copy(showSearch = show) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setReplaceText(text: String) {
        _uiState.update { it.copy(replaceText = text) }
    }

    fun setShowToc(show: Boolean) {
        _uiState.update { it.copy(showToc = show) }
    }

    fun setTocMode(mode: String) {
        _uiState.update { it.copy(tocMode = mode) }
    }

    fun setShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun setBackgroundColor(context: Context, color: String) {
        _uiState.update { it.copy(backgroundColor = color) }
        saveSettings(context)
    }

    fun setDarkTheme(isDark: Boolean) {
        _uiState.update { it.copy(isDarkTheme = isDark) }
    }
    fun setMatchResults(current: Int, total: Int) {
        _uiState.update { it.copy(currentMatch = current, totalMatches = total) }
    }

    fun setAutoSave(context: Context, enabled: Boolean) {
        _uiState.update { it.copy(autoSave = enabled) }
        saveSettings(context)
    }

    fun toggleStatusBar(context: Context) {
        _uiState.update { it.copy(showStatusBar = !it.showStatusBar) }
        saveSettings(context)
    }

    fun toggleSymbolBar(context: Context) {
        _uiState.update { it.copy(showSymbolBar = !it.showSymbolBar) }
        saveSettings(context)
    }

    fun setShowStatusBar(context: Context, show: Boolean) {
        _uiState.update { it.copy(showStatusBar = show) }
        saveSettings(context)
    }

    fun setShowSymbolBar(context: Context, show: Boolean) {
        _uiState.update { it.copy(showSymbolBar = show) }
        saveSettings(context)
    }

    fun setShowFileProperties(show: Boolean) {
        _uiState.update { it.copy(showFileProperties = show) }
    }

    fun setShowRenameDialog(show: Boolean) {
        _uiState.update { it.copy(showRenameDialog = show) }
    }

    fun setShowExitConfirmation(show: Boolean) {
        _uiState.update { it.copy(showExitConfirmation = show) }
    }

    fun setUiColor(context: Context, color: String) {
        _uiState.update { it.copy(uiColor = color) }
        saveSettings(context)
    }

    fun setTocColor(context: Context, color: String) {
        _uiState.update { it.copy(tocColor = color) }
        saveSettings(context)
    }

    fun setSearchColor(context: Context, color: String) {
        _uiState.update { it.copy(searchColor = color) }
        saveSettings(context)
    }

    fun setMenuColor(context: Context, color: String) {
        _uiState.update { it.copy(menuColor = color) }
        saveSettings(context)
    }

    fun setSearchMatchBackgroundColor(context: Context, color: String) {
        _uiState.update { it.copy(searchMatchBackgroundColor = color) }
        saveSettings(context)
    }

    fun setLineSpacingMultiplier(context: Context, multiplier: Float) {
        _uiState.update { it.copy(lineSpacingMultiplier = multiplier) }
        saveSettings(context)
    }

    fun setLineSpacingExtra(context: Context, extra: Float) {
        _uiState.update { it.copy(lineSpacingExtra = extra) }
        saveSettings(context)
    }

    fun setHorizontalPadding(context: Context, padding: Float) {
        _uiState.update { it.copy(horizontalPadding = padding) }
        saveSettings(context)
    }

    fun setWrapLineSpacing(context: Context, extra: Float, multiplier: Float) {
        _uiState.update { it.copy(wrapLineSpacingExtra = extra, wrapLineSpacingMultiplier = multiplier) }
        saveSettings(context)
    }

    fun setHighlightCurrentLine(context: Context, enabled: Boolean) {
        _uiState.update { it.copy(highlightCurrentLine = enabled) }
        saveSettings(context)
    }

    fun setCurrentLineBackgroundColor(context: Context, color: String) {
        _uiState.update { it.copy(currentLineBackgroundColor = color) }
        saveSettings(context)
    }

    fun setCursorColor(context: Context, color: String) {
        _uiState.update { it.copy(cursorColor = color) }
        saveSettings(context)
    }

    fun setHandleColor(context: Context, color: String) {
        _uiState.update { it.copy(handleColor = color) }
        saveSettings(context)
    }

    fun setCursorWidth(context: Context, width: Float) {
        _uiState.update { it.copy(cursorWidth = width) }
        saveSettings(context)
    }

    fun setHandleStyle(context: Context, style: String) {
        _uiState.update { it.copy(handleStyle = style) }
        saveSettings(context)
    }

    fun setSearchAsRegExp(context: Context, enabled: Boolean) {
        _uiState.update { it.copy(searchAsRegExp = enabled) }
        saveSettings(context)
    }

    fun setSearchWholeWord(context: Context, enabled: Boolean) {
        _uiState.update { it.copy(searchWholeWord = enabled) }
        saveSettings(context)
    }

    fun setSearchMatchCase(context: Context, enabled: Boolean) {
        _uiState.update { it.copy(searchMatchCase = enabled) }
        saveSettings(context)
    }

    fun setFontFamily(context: Context, fontFamily: String) {
        _uiState.update { it.copy(fontFamily = fontFamily) }
        saveSettings(context)
    }

    fun setScrollbarColor(context: Context, color: String) {
        _uiState.update { it.copy(scrollbarColor = color) }
        saveSettings(context)
    }

    fun setShowScrollLineInfo(context: Context, show: Boolean) {
        _uiState.update { it.copy(showScrollLineInfo = show) }
        saveSettings(context)
    }

    fun setScrollbarStyle(context: Context, style: String) {
        _uiState.update { it.copy(scrollbarStyle = style) }
        saveSettings(context)
    }

    fun setKeyboardAdjust(context: Context, enabled: Boolean) {
        _uiState.update { it.copy(keyboardAdjust = enabled) }
        saveSettings(context)
    }

    fun setSymbolBarColor(context: Context, color: String) {
        _uiState.update { it.copy(symbolBarColor = color) }
        saveSettings(context)
    }

    fun setSymbolTextColor(context: Context, color: String) {
        _uiState.update { it.copy(symbolTextColor = color) }
        saveSettings(context)
    }

    fun setSymbolBarStyle(context: Context, style: String) {
        _uiState.update { it.copy(symbolBarStyle = style) }
        saveSettings(context)
    }

    fun renameFile(newName: String): Boolean {
        val currentFile = File(_uiState.value.filePath)
        val parent = currentFile.parentFile
        val newFile = File(parent, newName)
        return if (currentFile.renameTo(newFile)) {
            _uiState.value = _uiState.value.copy(
                filePath = newFile.absolutePath,
                fileName = newFile.name
            )
            true
        } else {
            false
        }
    }

    fun moveToRecycleBin(): Boolean {
        return try {
            val file = File(_uiState.value.filePath)
            if (!file.exists()) return false
            
            // Assume the notes root is the parent of the first folder that doesn't start with '.'
            // Or more simply, let's just create a .recycle folder in the same directory as the file for now,
            // or go up until we find a reasonable root.
            // Let's go with a sibling ".recycle" folder in the same directory.
            val parent = file.parentFile ?: return false
            val recycleDir = File(parent, ".recycle")
            if (!recycleDir.exists()) {
                recycleDir.mkdirs()
            }
            
            val targetFile = File(recycleDir, file.name)
            // If target exists, append timestamp
            val finalTarget = if (targetFile.exists()) {
                File(recycleDir, "${System.currentTimeMillis()}_${file.name}")
            } else {
                targetFile
            }
            
            file.renameTo(finalTarget)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetSettings(context: Context) {
        _uiState.update { it.copy(
            fontSize = 18f,
            showLineNumbers = true,
            wordWrap = false,
            backgroundColor = "#FFFFFF",
            autoSave = true,
            showStatusBar = true,
            showSymbolBar = true,
            uiColor = "#F5F5F5",
            tocColor = "#FFFFFF",
            searchColor = "#F5F5F5",
            menuColor = "#FFFFFF",
            searchMatchBackgroundColor = "#FFF59D",
            lineSpacingMultiplier = 1.0f,
            lineSpacingExtra = 0f,
            wrapLineSpacingMultiplier = 1.0f,
            wrapLineSpacingExtra = 0f,
            horizontalPadding = 12f,
            highlightCurrentLine = true,
            currentLineBackgroundColor = "#10000000",
            cursorColor = "#FF000000",
            handleColor = "#FF000000",
            cursorWidth = 2.0f,
            handleStyle = "side_drop",
            searchAsRegExp = false,
            searchWholeWord = false,
            searchMatchCase = false,
            fontFamily = "Monospace",
            scrollbarColor = "#A0888888",
            showScrollLineInfo = true,
            scrollbarStyle = "default",
            keyboardAdjust = true,
            symbolBarColor = "#F5F5F5",
            symbolTextColor = "#FF000000",
            symbolBarStyle = "rounded"
        ) }
        saveSettings(context)
    }

    fun getFileDetails(): Map<String, String> {
        val file = File(_uiState.value.filePath)
        val details = mutableMapOf<String, String>()
        details["文件名"] = file.name
        details["路径"] = file.absolutePath
        details["大小"] = "${file.length()} 字节"
        details["最后修改"] = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))
        return details
    }

    private fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences("editor_settings", Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("fontSize", _uiState.value.fontSize.toDouble())
            put("showLineNumbers", _uiState.value.showLineNumbers)
            put("wordWrap", _uiState.value.wordWrap)
            put("backgroundColor", _uiState.value.backgroundColor)
            put("autoSave", _uiState.value.autoSave)
            put("showStatusBar", _uiState.value.showStatusBar)
            put("showSymbolBar", _uiState.value.showSymbolBar)
            put("uiColor", _uiState.value.uiColor)
            put("tocColor", _uiState.value.tocColor)
            put("searchColor", _uiState.value.searchColor)
            put("menuColor", _uiState.value.menuColor)
            put("searchMatchBackgroundColor", _uiState.value.searchMatchBackgroundColor)
            put("lineSpacingMultiplier", _uiState.value.lineSpacingMultiplier.toDouble())
            put("lineSpacingExtra", _uiState.value.lineSpacingExtra.toDouble())
            put("wrapLineSpacingMultiplier", _uiState.value.wrapLineSpacingMultiplier.toDouble())
            put("wrapLineSpacingExtra", _uiState.value.wrapLineSpacingExtra.toDouble())
            put("horizontalPadding", _uiState.value.horizontalPadding.toDouble())
            put("highlightCurrentLine", _uiState.value.highlightCurrentLine)
            put("currentLineBackgroundColor", _uiState.value.currentLineBackgroundColor)
            put("cursorColor", _uiState.value.cursorColor)
            put("handleColor", _uiState.value.handleColor)
            put("cursorWidth", _uiState.value.cursorWidth.toDouble())
            put("handleStyle", _uiState.value.handleStyle)
            put("searchAsRegExp", _uiState.value.searchAsRegExp)
            put("searchWholeWord", _uiState.value.searchWholeWord)
            put("searchMatchCase", _uiState.value.searchMatchCase)
            put("fontFamily", _uiState.value.fontFamily)
            put("scrollbarColor", _uiState.value.scrollbarColor)
            put("showScrollLineInfo", _uiState.value.showScrollLineInfo)
            put("scrollbarStyle", _uiState.value.scrollbarStyle)
            put("keyboardAdjust", _uiState.value.keyboardAdjust)
            put("symbolBarColor", _uiState.value.symbolBarColor)
            put("symbolTextColor", _uiState.value.symbolTextColor)
            put("symbolBarStyle", _uiState.value.symbolBarStyle)
        }
        prefs.edit().putString("settings_json", json.toString()).apply()
    }

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("editor_settings", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("settings_json", null) ?: return
        try {
            val json = JSONObject(jsonStr)
            _uiState.value = _uiState.value.copy(
                fontSize = json.optDouble("fontSize", 18.0).toFloat(),
                showLineNumbers = json.optBoolean("showLineNumbers", true),
                wordWrap = json.optBoolean("wordWrap", false),
                backgroundColor = json.optString("backgroundColor", "#FFFFFF"),
                autoSave = json.optBoolean("autoSave", true),
                showStatusBar = json.optBoolean("showStatusBar", true),
                showSymbolBar = json.optBoolean("showSymbolBar", true),
                uiColor = json.optString("uiColor", "#F5F5F5"),
                tocColor = json.optString("tocColor", "#FFFFFF"),
                searchColor = json.optString("searchColor", "#F5F5F5"),
                menuColor = json.optString("menuColor", "#FFFFFF"),
                searchMatchBackgroundColor = json.optString("searchMatchBackgroundColor", "#FFF59D"),
                lineSpacingMultiplier = json.optDouble("lineSpacingMultiplier", 1.0).toFloat(),
                lineSpacingExtra = json.optDouble("lineSpacingExtra", 0.0).toFloat(),
                wrapLineSpacingMultiplier = json.optDouble("wrapLineSpacingMultiplier", 1.0).toFloat(),
                wrapLineSpacingExtra = json.optDouble("wrapLineSpacingExtra", 0.0).toFloat(),
            horizontalPadding = json.optDouble("horizontalPadding", 12.0).toFloat(),
            highlightCurrentLine = json.optBoolean("highlightCurrentLine", true),
            currentLineBackgroundColor = json.optString("currentLineBackgroundColor", "#10000000"),
            cursorColor = json.optString("cursorColor", "#FF000000"),
            handleColor = json.optString("handleColor", "#FF000000"),
            cursorWidth = json.optDouble("cursorWidth", 2.0).toFloat(),
            handleStyle = json.optString("handleStyle", "side_drop"),
            searchAsRegExp = json.optBoolean("searchAsRegExp", false),
            searchWholeWord = json.optBoolean("searchWholeWord", false),
            searchMatchCase = json.optBoolean("searchMatchCase", false),
            fontFamily = json.optString("fontFamily", "Monospace"),
            scrollbarColor = json.optString("scrollbarColor", "#A0888888"),
            showScrollLineInfo = json.optBoolean("showScrollLineInfo", true),
            scrollbarStyle = json.optString("scrollbarStyle", "default"),
            keyboardAdjust = json.optBoolean("keyboardAdjust", true),
            symbolBarColor = json.optString("symbolBarColor", "#F5F5F5"),
            symbolTextColor = json.optString("symbolTextColor", "#FF000000"),
            symbolBarStyle = json.optString("symbolBarStyle", "rounded")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applySettingsFromJson(context: Context, jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            _uiState.value = _uiState.value.copy(
                fontSize = json.optDouble("fontSize", _uiState.value.fontSize.toDouble()).toFloat(),
                showLineNumbers = json.optBoolean("showLineNumbers", _uiState.value.showLineNumbers),
                wordWrap = json.optBoolean("wordWrap", _uiState.value.wordWrap),
                backgroundColor = json.optString("backgroundColor", _uiState.value.backgroundColor),
                autoSave = json.optBoolean("autoSave", _uiState.value.autoSave),
                showStatusBar = json.optBoolean("showStatusBar", _uiState.value.showStatusBar),
                showSymbolBar = json.optBoolean("showSymbolBar", _uiState.value.showSymbolBar),
                uiColor = json.optString("uiColor", _uiState.value.uiColor),
                tocColor = json.optString("tocColor", _uiState.value.tocColor),
                searchColor = json.optString("searchColor", _uiState.value.searchColor),
                menuColor = json.optString("menuColor", _uiState.value.menuColor),
                searchMatchBackgroundColor = json.optString("searchMatchBackgroundColor", _uiState.value.searchMatchBackgroundColor),
                lineSpacingMultiplier = json.optDouble("lineSpacingMultiplier", _uiState.value.lineSpacingMultiplier.toDouble()).toFloat(),
                lineSpacingExtra = json.optDouble("lineSpacingExtra", _uiState.value.lineSpacingExtra.toDouble()).toFloat(),
                wrapLineSpacingMultiplier = json.optDouble("wrapLineSpacingMultiplier", _uiState.value.wrapLineSpacingMultiplier.toDouble()).toFloat(),
                wrapLineSpacingExtra = json.optDouble("wrapLineSpacingExtra", _uiState.value.wrapLineSpacingExtra.toDouble()).toFloat(),
                horizontalPadding = json.optDouble("horizontalPadding", _uiState.value.horizontalPadding.toDouble()).toFloat(),
                highlightCurrentLine = json.optBoolean("highlightCurrentLine", _uiState.value.highlightCurrentLine),
                currentLineBackgroundColor = json.optString("currentLineBackgroundColor", _uiState.value.currentLineBackgroundColor),
                cursorColor = json.optString("cursorColor", _uiState.value.cursorColor),
                handleColor = json.optString("handleColor", _uiState.value.handleColor),
                cursorWidth = json.optDouble("cursorWidth", _uiState.value.cursorWidth.toDouble()).toFloat(),
                handleStyle = json.optString("handleStyle", _uiState.value.handleStyle),
                searchAsRegExp = json.optBoolean("searchAsRegExp", _uiState.value.searchAsRegExp),
                searchWholeWord = json.optBoolean("searchWholeWord", _uiState.value.searchWholeWord),
                searchMatchCase = json.optBoolean("searchMatchCase", _uiState.value.searchMatchCase),
                fontFamily = json.optString("fontFamily", _uiState.value.fontFamily),
                scrollbarColor = json.optString("scrollbarColor", _uiState.value.scrollbarColor),
                showScrollLineInfo = json.optBoolean("showScrollLineInfo", _uiState.value.showScrollLineInfo),
                scrollbarStyle = json.optString("scrollbarStyle", _uiState.value.scrollbarStyle),
                keyboardAdjust = json.optBoolean("keyboardAdjust", _uiState.value.keyboardAdjust),
                symbolBarColor = json.optString("symbolBarColor", _uiState.value.symbolBarColor),
                symbolTextColor = json.optString("symbolTextColor", _uiState.value.symbolTextColor),
                symbolBarStyle = json.optString("symbolBarStyle", _uiState.value.symbolBarStyle)
            )
            saveSettings(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getSettingsJson(): String {
        return JSONObject().apply {
            put("fontSize", _uiState.value.fontSize.toDouble())
            put("showLineNumbers", _uiState.value.showLineNumbers)
            put("wordWrap", _uiState.value.wordWrap)
            put("backgroundColor", _uiState.value.backgroundColor)
            put("autoSave", _uiState.value.autoSave)
            put("showStatusBar", _uiState.value.showStatusBar)
            put("showSymbolBar", _uiState.value.showSymbolBar)
            put("uiColor", _uiState.value.uiColor)
            put("tocColor", _uiState.value.tocColor)
            put("searchColor", _uiState.value.searchColor)
            put("menuColor", _uiState.value.menuColor)
            put("searchMatchBackgroundColor", _uiState.value.searchMatchBackgroundColor)
            put("lineSpacingMultiplier", _uiState.value.lineSpacingMultiplier.toDouble())
            put("lineSpacingExtra", _uiState.value.lineSpacingExtra.toDouble())
            put("wrapLineSpacingMultiplier", _uiState.value.wrapLineSpacingMultiplier.toDouble())
            put("wrapLineSpacingExtra", _uiState.value.wrapLineSpacingExtra.toDouble())
            put("horizontalPadding", _uiState.value.horizontalPadding.toDouble())
            put("highlightCurrentLine", _uiState.value.highlightCurrentLine)
            put("currentLineBackgroundColor", _uiState.value.currentLineBackgroundColor)
            put("cursorColor", _uiState.value.cursorColor)
            put("handleColor", _uiState.value.handleColor)
            put("cursorWidth", _uiState.value.cursorWidth.toDouble())
            put("handleStyle", _uiState.value.handleStyle)
            put("searchAsRegExp", _uiState.value.searchAsRegExp)
            put("searchWholeWord", _uiState.value.searchWholeWord)
            put("searchMatchCase", _uiState.value.searchMatchCase)
            put("fontFamily", _uiState.value.fontFamily)
            put("scrollbarColor", _uiState.value.scrollbarColor)
            put("showScrollLineInfo", _uiState.value.showScrollLineInfo)
            put("scrollbarStyle", _uiState.value.scrollbarStyle)
            put("keyboardAdjust", _uiState.value.keyboardAdjust)
            put("symbolBarColor", _uiState.value.symbolBarColor)
            put("symbolTextColor", _uiState.value.symbolTextColor)
            put("symbolBarStyle", _uiState.value.symbolBarStyle)
        } .toString(4)
    }
}
