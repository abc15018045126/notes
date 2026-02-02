package com.abc15018045126.capacitor.soraeditor

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.FrameLayout
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.style.builtin.HandleStyleDrop
import io.github.abc15018045126.sora.widget.style.builtin.HandleStyleSideDrop
import io.github.abc15018045126.sora.widget.style.builtin.HandleStyleNone

@CapacitorPlugin(name = "SoraEditor")
class SoraEditorPlugin : Plugin() {

    private var editor: CodeEditor? = null

    @PluginMethod
    fun start(call: PluginCall) {
        val content = call.getString("content") ?: ""
        val topMargin = call.getInt("top") ?: 0
        val leftMargin = call.getInt("left") ?: 0
        val rightMargin = call.getInt("right") ?: 0
        val bottomMargin = call.getInt("bottom") ?: 0
        val width = call.getInt("width") ?: -1 // -1 = MATCH_PARENT
        val height = call.getInt("height") ?: -1 
        val fontSize = call.getFloat("fontSize") ?: 18f
        val showLineNumbers = call.getBoolean("showLineNumbers") ?: true
        val wordWrap = call.getBoolean("wordWrap") ?: false
        val editable = call.getBoolean("editable") ?: true
        val bgColorStr = call.getString("backgroundColor") // e.g. "#FFFFFF"
        
        activity.runOnUiThread {
            if (editor == null) {
                editor = CodeEditor(context).apply {
                    setTextSize(fontSize)
                    setTypefaceText(Typeface.MONOSPACE)
                    isLineNumberEnabled = showLineNumbers
                    isWordwrap = wordWrap
                    isEditable = editable
                    
                    // Initial setup for new features
                    setLineSpacing(call.getFloat("lineSpacingExtra") ?: 0f, call.getFloat("lineSpacingMultiplier") ?: 1.0f)
                    setWrapLineSpacing(call.getFloat("wrapLineSpacingExtra") ?: 0f, call.getFloat("wrapLineSpacingMultiplier") ?: 1.0f)
                    isHighlightCurrentLine = call.getBoolean("highlightCurrentLine") ?: true

                    isDisplayLnPanel = call.getBoolean("showScrollLineInfo") ?: true
                    
                    val hPadding = call.getFloat("horizontalPadding") ?: 12f
                    setDividerMargin(0f, hPadding * dpUnit)
                    extraMarginRight = hPadding * dpUnit

                    setLineNumberMarginLeft(hPadding * dpUnit)

                    var startX = 0f
                    var startY = 0f
                    var startTime = 0L
                    
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                startX = event.x
                                startY = event.y
                                startTime = System.currentTimeMillis()
                            }
                            android.view.MotionEvent.ACTION_UP -> {
                                val duration = System.currentTimeMillis() - startTime
                                val dist = Math.sqrt(Math.pow((event.x - startX).toDouble(), 2.0) + Math.pow((event.y - startY).toDouble(), 2.0))
                                if (duration < 300 && dist < 20) {
                                    notifyListeners("onEditorClick", JSObject())
                                }
                            }
                        }
                        false // Allow editor to handle the event too
                    }
                    subscribeEvent(io.github.abc15018045126.sora.event.ContentChangeEvent::class.java) { event, _ ->
                        notifyListeners("onContentChange", JSObject())
                    }
                }
                
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                activity.addContentView(editor, params)
            }

            editor!!.isLineNumberEnabled = showLineNumbers
            editor!!.isWordwrap = wordWrap
            editor!!.isEditable = editable
            
            if (bgColorStr != null) {
                try {
                    val color = Color.parseColor(bgColorStr)
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                    
                    editor!!.colorScheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, color)
                    editor!!.colorScheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, color)
                    
                    if (luminance < 0.5) {
                        editor!!.colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, Color.WHITE)
                        editor!!.colorScheme.setColor(EditorColorScheme.LINE_NUMBER, Color.GRAY)
                    } else {
                        editor!!.colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, Color.BLACK)
                        editor!!.colorScheme.setColor(EditorColorScheme.LINE_NUMBER, Color.DKGRAY)
                    }
                } catch (e: Exception) {}
            }

            val searchMatchBgStr = call.getString("searchMatchBackgroundColor")
            if (searchMatchBgStr != null) {
                try {
                    val color = Color.parseColor(searchMatchBgStr)
                    editor!!.colorScheme.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, color)
                } catch (e: Exception) {}
            }

            val density = context.resources.displayMetrics.density
            val params = editor!!.layoutParams as FrameLayout.LayoutParams
            params.topMargin = (topMargin * density).toInt()
            params.leftMargin = (leftMargin * density).toInt()
            params.rightMargin = (rightMargin * density).toInt()
            params.bottomMargin = (bottomMargin * density).toInt()
            if (width >= 0) params.width = (width * density).toInt() else params.width = FrameLayout.LayoutParams.MATCH_PARENT
            if (height >= 0) params.height = (height * density).toInt() else params.height = FrameLayout.LayoutParams.MATCH_PARENT
            
            editor!!.layoutParams = params

            if (call.hasOption("content")) {
                val currentText = editor!!.text.toString()
                if (currentText != content) {
                     editor!!.setText(content)
                }
            }
            
            editor!!.setTextSize(fontSize)
            editor!!.isLineNumberEnabled = showLineNumbers
            editor!!.isWordwrap = wordWrap
            editor!!.isEditable = editable
            
            // Re-apply spacing/padding in update
            editor!!.setLineSpacing(call.getFloat("lineSpacingExtra") ?: 0f, call.getFloat("lineSpacingMultiplier") ?: 1.0f)
            editor!!.setWrapLineSpacing(call.getFloat("wrapLineSpacingExtra") ?: 0f, call.getFloat("wrapLineSpacingMultiplier") ?: 1.0f)
            editor!!.isHighlightCurrentLine = call.getBoolean("highlightCurrentLine") ?: true

            editor!!.isDisplayLnPanel = call.getBoolean("showScrollLineInfo") ?: true
            
            val hPadding = call.getFloat("horizontalPadding") ?: 12f
            editor!!.setDividerMargin(0f, hPadding * editor!!.dpUnit)
            editor!!.extraMarginRight = hPadding * editor!!.dpUnit

            editor!!.setLineNumberMarginLeft(hPadding * editor!!.dpUnit)

            // Handle Font Family
            val fontFamily = call.getString("fontFamily") ?: "Monospace"
            when (fontFamily) {
                "JetBrains Mono" -> editor!!.setTypefaceText(Typeface.createFromAsset(context.assets, "JetBrainsMono-Regular.ttf"))
                "Ubuntu" -> editor!!.setTypefaceText(Typeface.createFromAsset(context.assets, "Ubuntu-Regular.ttf"))
                "Roboto" -> editor!!.setTypefaceText(Typeface.createFromAsset(context.assets, "Roboto-Regular.ttf"))
                else -> editor!!.setTypefaceText(Typeface.MONOSPACE)
            }

            // Handle Colors
            val currentLineBg = call.getString("currentLineBackgroundColor")
            if (currentLineBg != null) {
                try { editor!!.colorScheme.setColor(EditorColorScheme.CURRENT_LINE, Color.parseColor(currentLineBg)) } catch(e: Exception) {}
            }
            
            val cursorColor = call.getString("cursorColor")
            if (cursorColor != null) {
                try { editor!!.colorScheme.setColor(EditorColorScheme.SELECTION_INSERT, Color.parseColor(cursorColor)) } catch(e: Exception) {}
            }
            
            val handleColor = call.getString("handleColor")
            if (handleColor != null) {
                try { editor!!.colorScheme.setColor(EditorColorScheme.SELECTION_HANDLE, Color.parseColor(handleColor)) } catch(e: Exception) {}
            }
            
            val scrollbarColor = call.getString("scrollbarColor")
            if (scrollbarColor != null) {
                try { 
                    val sColor = Color.parseColor(scrollbarColor)
                    editor!!.colorScheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB, sColor)
                    editor!!.colorScheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, sColor)
                } catch(e: Exception) {}
            }

            val scrollbarStyle = call.getString("scrollbarStyle") ?: "default"
            if (scrollbarStyle == "rounded") {
                val sColor = try { Color.parseColor(scrollbarColor ?: "#A0888888") } catch(e: Exception) { 0xFFA0888888.toInt() }
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 100f
                    setColor(sColor)
                }
                editor!!.renderer.setVerticalScrollbarThumbDrawable(drawable)
                editor!!.renderer.setHorizontalScrollbarThumbDrawable(drawable)
            } else {
                editor!!.renderer.setVerticalScrollbarThumbDrawable(null)
                editor!!.renderer.setHorizontalScrollbarThumbDrawable(null)
            }

            editor!!.setCursorWidth((call.getFloat("cursorWidth") ?: 2f) * editor!!.dpUnit / 2f)

            val handleStyle = call.getString("handleStyle") ?: "side_drop"
            editor!!.setSelectionHandleStyle(when (handleStyle) {
                "drop" -> HandleStyleDrop(context)
                "none" -> HandleStyleNone()
                else -> HandleStyleSideDrop(context)
            })

            if (call.hasOption("keyboardAdjust")) {
                val adjust = call.getBoolean("keyboardAdjust") ?: true
                activity.runOnUiThread {
                    if (adjust) {
                        activity.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    } else {
                        activity.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                    }
                }
            }

            val symbolBarColor = call.getString("symbolBarColor") ?: "#F5F5F5"
            val symbolTextColor = call.getString("symbolTextColor") ?: "#FF000000"
            val symbolBarStyle = call.getString("symbolBarStyle") ?: "rounded"

            editor!!.visibility = View.VISIBLE
            editor!!.bringToFront()
            
            if (call.hasOption("selectionLine") || call.hasOption("selectionColumn")) {
                 val l = call.getInt("selectionLine") ?: 0
                 val c = call.getInt("selectionColumn") ?: 0
                 editor!!.postDelayed({
                    try {
                        editor!!.setSelection(l, c)
                        editor!!.ensureSelectionVisible()
                    } catch(e: Exception){
                    }
                }, 150)
            }
        }
        call.resolve()
    }

    @PluginMethod
    fun close(call: PluginCall) {
        activity.runOnUiThread {
            editor?.visibility = View.GONE
        }
        call.resolve()
    }

    @PluginMethod
    fun getText(call: PluginCall) {
        val ret = JSObject()
        activity.runOnUiThread {
            ret.put("content", editor?.text?.toString() ?: "")
            call.resolve(ret)
        }
    }

    @PluginMethod
    fun getSelection(call: PluginCall) {
        val ret = JSObject()
        activity.runOnUiThread {
            if (editor != null) {
                val cursor = editor!!.cursor!!
                ret.put("line", cursor.leftLine)
                ret.put("column", cursor.leftColumn)

            }
            call.resolve(ret)
        }
    }
    
    @PluginMethod
    fun setText(call: PluginCall) {
        val content = call.getString("content") ?: ""
        activity.runOnUiThread {
            editor?.setText(content)
        }
        call.resolve()
    }

    @PluginMethod
    fun setSelection(call: PluginCall) {
        val line = call.getInt("line") ?: 0
        val column = call.getInt("column") ?: 0
        activity.runOnUiThread {
            editor?.post {
                try {
                    editor?.setSelection(line, column)
                    editor?.ensureSelectionVisible()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        call.resolve()
    }

    @PluginMethod
    fun undo(call: PluginCall) {
        activity.runOnUiThread {
            editor?.undo()
        }
        call.resolve()
    }

    @PluginMethod
    fun redo(call: PluginCall) {
        activity.runOnUiThread {
            editor?.redo()
        }
        call.resolve()
    }

    @PluginMethod
    fun openEditor(call: PluginCall) {
        val filePath = call.getString("filePath") ?: ""
        val autoFocus = call.getBoolean("autoFocus") ?: false
        if (filePath.isEmpty()) {
            call.reject("File path is required")
            return
        }
        activity.runOnUiThread {
            val intent = android.content.Intent(context, com.abc15018045126.capacitor.soraeditor.compose.ComposeEditorActivity::class.java)
            intent.putExtra("FILE_PATH", filePath)
            intent.putExtra("AUTO_FOCUS", autoFocus)
            activity.startActivity(intent)
            call.resolve()
        }
    }
}
