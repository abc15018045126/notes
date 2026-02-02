package io.github.abc15018045126.sora.widget

import android.os.Looper
import io.github.abc15018045126.sora.event.SelectionChangeEvent
import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager
import io.github.abc15018045126.sora.lang.analysis.StyleReceiver
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.brackets.BracketsProvider
import io.github.abc15018045126.sora.lang.brackets.PairedBracket
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticsContainer
import io.github.abc15018045126.sora.lang.styling.Styles
import java.lang.ref.WeakReference

class EditorStyleDelegate(editor: CodeEditor) : StyleReceiver {

    private val editorRef: WeakReference<CodeEditor> = WeakReference(editor)
    var foundBracketPair: PairedBracket? = null
        private set
    private var bracketsProvider: BracketsProvider? = null

    init {
        editor.subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
            if (!event.isSelected) {
                postUpdateBracketPair()
            }
        }
    }

    fun onTextChange() {
        // Should we do this?
        // bracketsProvider = null
        // foundBracketPair = null
    }

    fun postUpdateBracketPair() {
        runOnUiThread {
            val provider = bracketsProvider
            val editor = editorRef.get()
            if (provider != null && editor != null && !editor.cursor!!.isSelected() && editor.isHighlightBracketPair()) {
                foundBracketPair = provider.getPairedBracketAt(editor.text, editor.cursor!!.left)

                editor.invalidate()
            }
        }
    }

    fun reset() {
        foundBracketPair = null
        bracketsProvider = null
    }

    private fun runOnUiThread(operation: Runnable) {
        val editor = editorRef.get() ?: return
        if (Looper.getMainLooper().thread === Thread.currentThread()) {
            operation.run()
        } else {
            io.github.abc15018045126.sora.util.EditorHandler.post {
                if (!editor.isReleased) {
                    operation.run()
                }
            }
        }
    }


    override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?) {
        setStyles(sourceManager, styles, null)
    }

    override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?, action: Runnable?) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage?.analyzeManager) {

            runOnUiThread {
                action?.run()
                editor.setStyles(styles)
            }
        }
    }

    override fun setDiagnostics(sourceManager: AnalyzeManager, diagnostics: DiagnosticsContainer?) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage?.analyzeManager) {

            runOnUiThread { editor.diagnostics = diagnostics }
        }
    }

    override fun updateBracketProvider(sourceManager: AnalyzeManager, provider: BracketsProvider?) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage?.analyzeManager && bracketsProvider !== provider) {

            this.bracketsProvider = provider
            postUpdateBracketPair()
        }
    }

    override fun updateStyles(sourceManager: AnalyzeManager, styles: Styles, range: StyleUpdateRange) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage?.analyzeManager) {

            runOnUiThread { editor.updateStyles(styles, range) }
        }
    }

    fun clearFoundBracketPair() {
        this.foundBracketPair = null
    }
}
