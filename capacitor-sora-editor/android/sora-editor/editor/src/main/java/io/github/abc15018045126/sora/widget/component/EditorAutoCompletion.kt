package io.github.abc15018045126.sora.widget.component

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListAdapter
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.event.*
import io.github.abc15018045126.sora.lang.Language
import io.github.abc15018045126.sora.lang.completion.CompletionItem
import io.github.abc15018045126.sora.lang.completion.CompletionPublisher
import io.github.abc15018045126.sora.lang.completion.highlightMatchLabel
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.base.EditorPopupWindow
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.snippet.SnippetController

/**
 * Auto-completion component for CodeEditor.
 *
 * This window is displayed when the user types text and the language can provide completions.
 */
class EditorAutoCompletion(editor: CodeEditor) :
    EditorPopupWindow(editor, FEATURE_SCROLL_AS_CONTENT or FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED),
    EditorBuiltinComponent {

    private val eventManager = editor.createSubEventManager()
    private var thread: CompletionThread? = null

    override var isEnabled = true
        set(value) {
            field = value
            eventManager.isEnabled = value
            if (!value) {
                dismiss()
            }
        }

    var maxHeight: Int = (editor.dpUnit * 200).toInt()
        set(value) {
            field = value
            if (isShowing) {
                updateCompletionWindowPosition()
            }
        }

    var adapter: EditorCompletionAdapter? = null
        set(value) {
            val newAdapter = value ?: DefaultCompletionItemAdapter()
            field = newAdapter
            newAdapter.attachValues(this, emptyList())
            val currentLayout = layout
            if (currentLayout != null) {
                @Suppress("UNCHECKED_CAST")
                (currentLayout.getCompletionList() as AdapterView<ListAdapter>).adapter = newAdapter
            }
        }

    var layout: CompletionLayout? = null
        set(value) {
            val newLayout = value ?: DefaultCompletionLayout()
            field = newLayout
            newLayout.setEditorCompletion(this)
            setContentView(newLayout.inflate(editor.context))
            applyColorScheme()
            val currentAdapter = adapter
            if (currentAdapter != null) {
                @Suppress("UNCHECKED_CAST")
                (newLayout.getCompletionList() as AdapterView<ListAdapter>).adapter = currentAdapter
            }
        }

    var currentPosition: Int = -1
        private set

    val isCompletionInProgress: Boolean
        get() = thread != null

    init {
        adapter = DefaultCompletionItemAdapter()
        layout = DefaultCompletionLayout()
        popup.animationStyle = 0
        applyColorScheme()
        subscribeEvents()
    }

    private fun subscribeEvents() {
        eventManager.subscribeAlways(ContentChangeEvent::class.java, this::onContentChange)
        eventManager.subscribeAlways(ScrollEvent::class.java, this::onEditorScroll)
        eventManager.subscribeAlways(SelectionChangeEvent::class.java, this::onSelectionChange)
        eventManager.subscribeAlways(EditorFocusChangeEvent::class.java, this::onEditorFocusChange)
        eventManager.subscribeAlways(EditorReleaseEvent::class.java, this::onEditorRelease)
        eventManager.subscribeAlways(ColorSchemeUpdateEvent::class.java, this::onEditorColorChange)
    }

    private fun onEditorColorChange(event: ColorSchemeUpdateEvent) {
        applyColorScheme()
    }

    private fun onEditorRelease(event: EditorReleaseEvent) {
        isEnabled = false
    }

    private fun onEditorFocusChange(event: EditorFocusChangeEvent) {
        if (!event.isGainFocus) {
            dismiss()
        }
    }

    private fun onEditorScroll(event: ScrollEvent) {
        if (isShowing) {
            updateCompletionWindowPosition()
        }
    }

    private fun onSelectionChange(event: SelectionChangeEvent) {
        if (isShowing && event.cause != SelectionChangeEvent.CAUSE_IME) {
            dismiss()
        }
    }

    private fun onContentChange(event: ContentChangeEvent) {
        val snippetController: io.github.abc15018045126.sora.widget.snippet.SnippetController? = editor.snippetController
        if (editor.text.isInBatchEdit || !isEnabled || snippetController?.isInSnippet() == true) {
            return
        }
        val text = editor.text
        val cursor = editor.cursor!!

        if (cursor.isSelected()) {
            dismiss()
            return
        }
        
        if (event.action == ContentChangeEvent.ACTION_DELETE) {
            if (isShowing) {
                updateCompletions()
            }
            return
        }

        if (event.action == ContentChangeEvent.ACTION_INSERT) {
            requireCompletion()
            return
        }
        
        if (isShowing) {
            updateCompletions()
        }
    }

    fun requireCompletion() {
        val cursor = editor.cursor
        val line = cursor.leftLine
        val col = cursor.leftColumn
        val text = editor.text
        
        thread?.cancel()
        
        val lang = editor.editorLanguage!!
        val publisher = CompletionPublisher(editor.handler, {
            io.github.abc15018045126.sora.util.EditorHandler.post {
                if (editor.isReleased) return@post
                val currentThread = thread
                if (currentThread != null && !currentThread.isCancelled) {
                    onCompletionsProvided(currentThread.publisher.getItems(), false)
                }
            }

        }, lang.interruptionLevel)

        val t = CompletionThread(publisher, text, cursor.left(), lang)

        thread = t
        t.start()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onCompletionsProvided(items: List<CompletionItem>, results: Boolean) {
        if (items.isEmpty()) {
            if (results) {
                dismiss()
            }
            return
        }
        val sorted = items.highlightMatchLabel(editor.colorScheme)
        adapter?.attachValues(this, sorted)
        adapter?.notifyDataSetChanged()
        
        if (!isShowing) {
            show()
        }
        updateCompletionWindowPosition()
    }

    private fun updateCompletions() {
        requireCompletion()
    }

    private fun applyColorScheme() {
        val scheme = editor.colorScheme
        val gd = GradientDrawable()
        gd.cornerRadius = 4 * editor.dpUnit
        gd.setColor(scheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND))
        gd.setStroke((editor.dpUnit * 1).toInt(), scheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER))
        layout?.getCompletionList()?.background = gd
    }

    private fun updateCompletionWindowPosition() {
        if (!isShowing) return
        
        val cursor = editor.cursor
        val line = cursor.leftLine
        val col = cursor.leftColumn
        
        val charX = editor.getCharOffsetX(line, col)
        val charY = editor.getCharOffsetY(line, col)
        
        val dp = editor.dpUnit
        val rowHeight = editor.rowHeight
        
        val panelX = charX
        var panelY = charY + rowHeight
        
        val adapterCount = adapter?.count ?: 0
        val itemHeight = (dp * 40).toInt()
        val totalHeight = Math.min(maxHeight, adapterCount * itemHeight)
        
        if (panelY + totalHeight > editor.height) {
            panelY = charY - totalHeight
        }
        
        setSize(Math.min(editor.width, (dp * 250).toInt()), totalHeight)
        setLocationAbsolutely(panelX, panelY)
    }

    override fun show() {
        val snippetController: io.github.abc15018045126.sora.widget.snippet.SnippetController? = editor.snippetController
        if (!isEnabled || snippetController?.isInSnippet() == true || !editor.hasFocus() || editor.isInMouseMode) {

            return
        }
        super.show()
    }

    override fun dismiss() {
        super.dismiss()
        cancelCompletion()
    }

    fun cancelCompletion() {
        thread?.cancel()
        thread = null
    }

    fun hide() {
        dismiss()
    }

    fun selectCompletion(position: Int) {
        val item = adapter?.getItem(position) ?: return
        selectCompletion(item)
    }

    fun selectCompletion(item: CompletionItem) {
        val cursor = editor.cursor!!
        item.performCompletion(editor, editor.text, cursor.left())
        dismiss()
    }


    class CompletionThread(
        val publisher: CompletionPublisher,
        val text: Content,
        val position: CharPosition,
        val language: Language
    ) : Thread() {
        var isCancelled: Boolean = false
            private set

        fun cancel() {
            isCancelled = true
            publisher.cancel()
            interrupt()
        }

        override fun run() {
            try {
                language.requireAutoComplete(ContentReference(text), position, publisher, Bundle.EMPTY)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
