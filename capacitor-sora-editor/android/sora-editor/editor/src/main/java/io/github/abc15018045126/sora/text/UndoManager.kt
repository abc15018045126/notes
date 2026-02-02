package io.github.abc15018045126.sora.text

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Helper class for Content to take down modification
 * As well as provide Undo/Redo actions
 *
 * @author abc15018045126
 */
class UndoManager : ContentListener, Parcelable {

    private val actionStack: MutableList<ContentAction>
    private var undoEnabled: Boolean = false
    private var maxStackSize: Int = 0
    private var insertAction: InsertAction? = null
    private var deleteAction: DeleteAction? = null
    private var targetContent: Content? = null
    private var replaceMark: Boolean = false
    private var stackPointer: Int = 0
    private var ignoreModification: Boolean = false
    private var forceNewMultiAction: Boolean = false
    private var memorizedCursorRange: TextRange? = null

    /**
     * Create an UndoManager
     */
    constructor() {
        actionStack = ArrayList()
        replaceMark = false
        insertAction = null
        deleteAction = null
        stackPointer = 0
        ignoreModification = false
    }

    private constructor(parcel: Parcel) {
        maxStackSize = parcel.readInt()
        stackPointer = parcel.readInt()
        undoEnabled = parcel.readInt() > 0
        val count = parcel.readInt()
        actionStack = ArrayList(count)
        repeat(count) {
            actionStack.add(parcel.readParcelable<ContentAction>(UndoManager::class.java.classLoader)!!)
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(maxStackSize)
        parcel.writeInt(stackPointer)
        parcel.writeInt(if (undoEnabled) 1 else 0)
        parcel.writeInt(actionStack.size)
        for (contentAction in actionStack) {
            parcel.writeParcelable(contentAction, flags)
        }
    }

    /**
     * Check whether we are currently in undo/redo operations
     */
    val isModifyingContent: Boolean
        get() = ignoreModification

    /**
     * Undo on the given Content
     *
     * @param content Undo Target
     */
    fun undo(content: Content): TextRange? {
        if (canUndo() && !isModifyingContent) {
            ignoreModification = true
            val action = actionStack[stackPointer - 1]
            action.undo(content)
            stackPointer--
            ignoreModification = false
            return action.cursor
        }
        return null
    }

    /**
     * Redo on the given Content
     *
     * @param content Redo Target
     */
    fun redo(content: Content) {
        if (canRedo() && !isModifyingContent) {
            ignoreModification = true
            actionStack[stackPointer].redo(content)
            stackPointer++
            ignoreModification = false
        }
    }

    internal fun onExitBatchEdit() {
        forceNewMultiAction = true
        if (actionStack.isNotEmpty() && actionStack.last() is MultiAction) {
            val action = actionStack.last() as MultiAction
            if (action.actions.size == 1) {
                actionStack[actionStack.size - 1] = action.actions[0]
            }
        }
    }

    /**
     * Whether it can undo
     */
    fun canUndo(): Boolean {
        return isUndoEnabled && stackPointer > 0
    }

    /**
     * Whether it can redo
     */
    fun canRedo(): Boolean {
        return isUndoEnabled && stackPointer < actionStack.size
    }

    /**
     * Whether this UndoManager is enabled
     */
    var isUndoEnabled: Boolean
        get() = undoEnabled
        set(enabled) {
            undoEnabled = enabled
            if (!enabled) {
                cleanStack()
            }
        }

    /**
     * Get/Set a max stack size for this UndoManager
     */
    var maxUndoStackSize: Int
        get() = maxStackSize
        set(maxSize) {
            if (maxSize <= 0) {
                throw IllegalArgumentException("max size can not be zero or smaller.")
            }
            maxStackSize = maxSize
            cleanStack()
        }

    /**
     * Clean stack after add or state change
     * This is to limit stack size
     */
    private fun cleanStack() {
        if (!undoEnabled) {
            actionStack.clear()
            stackPointer = 0
        } else {
            while (stackPointer > 1 && actionStack.size > maxStackSize) {
                actionStack.removeAt(0)
                stackPointer--
            }
        }
    }

    /**
     * Clean the stack before pushing
     * If we are not at the end(Undo action executed), remove those actions
     */
    private fun cleanBeforePush() {
        while (stackPointer < actionStack.size) {
            actionStack.removeAt(actionStack.size - 1)
        }
    }

    /**
     * Push a new [ContentAction] to stack
     * It will merge actions if possible
     */
    private fun pushAction(content: Content, action: ContentAction) {
        if (!isUndoEnabled) return
        cleanBeforePush()
        if (content.isInBatchEdit) {
            if (actionStack.isEmpty()) {
                val a = MultiAction()
                a.addAction(action)
                a.cursor = action.cursor
                actionStack.add(a)
                stackPointer++
            } else {
                val a = actionStack.last()
                if (a is MultiAction && !forceNewMultiAction) {
                    a.addAction(action)
                } else {
                    val ac = MultiAction()
                    ac.addAction(action)
                    ac.cursor = action.cursor
                    actionStack.add(ac)
                    stackPointer++
                }
            }
        } else {
            if (actionStack.isEmpty()) {
                actionStack.add(action)
                stackPointer++
            } else {
                val last = actionStack.last()
                if (last.canMerge(action)) {
                    last.merge(action)
                } else {
                    actionStack.add(action)
                    stackPointer++
                }
            }
        }
        forceNewMultiAction = false
        cleanStack()
    }

    fun exitReplaceMode() {
        if (replaceMark && deleteAction != null) {
            pushAction(targetContent!!, deleteAction!!)
        }
        replaceMark = false
        targetContent = null
    }

    override fun beforeReplace(content: Content) {
        if (ignoreModification) return
        replaceMark = true
        targetContent = content
    }

    override fun afterInsert(
        content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int,
        insertedContent: CharSequence
    ) {
        if (ignoreModification) return
        val ins = InsertAction().apply {
            this.startLine = startLine
            this.startColumn = startColumn
            this.endLine = endLine
            this.endColumn = endColumn
            this.text = insertedContent
        }
        insertAction = ins
        if (replaceMark && deleteAction != null) {
            val rep = ReplaceAction().apply {
                this.delete = deleteAction
                this.insert = ins
                this.cursor = memorizedCursorRange
            }
            pushAction(content, rep)
        } else {
            ins.cursor = memorizedCursorRange
            pushAction(content, ins)
        }
        deleteAction = null
        insertAction = null
        replaceMark = false
    }

    override fun afterDelete(
        content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int,
        deletedContent: CharSequence
    ) {
        if (ignoreModification) return
        val del = DeleteAction().apply {
            this.endColumn = endColumn
            this.startColumn = startColumn
            this.endLine = endLine
            this.startLine = startLine
            this.text = deletedContent
            this.cursor = memorizedCursorRange
        }
        deleteAction = del
        if (!replaceMark) {
            pushAction(content, del)
        }
    }

    override fun beforeModification(content: Content) {
        if (!undoEnabled || !content.isCursorCreated() || (replaceMark && deleteAction != null)) {
            return
        }
        val cursor = content.cursor
        memorizedCursorRange = cursor.getRange()
    }

    /**
     * Base class of content actions
     */
    abstract class ContentAction : Parcelable {
        @JvmField
        @Transient
        var cursor: TextRange? = null

        abstract fun undo(content: Content)
        abstract fun redo(content: Content)
        abstract fun canMerge(action: ContentAction): Boolean
        abstract fun merge(action: ContentAction)
    }

    /**
     * Insert action model for UndoManager
     */
    class InsertAction : ContentAction {
        @JvmField
        var startLine: Int = 0
        @JvmField
        var endLine: Int = 0
        @JvmField
        var startColumn: Int = 0
        @JvmField
        var endColumn: Int = 0
        @JvmField
        @Transient
        var createTime: Long = System.currentTimeMillis()
        @JvmField
        var text: CharSequence? = null

        constructor()

        private constructor(parcel: Parcel) {
            startLine = parcel.readInt()
            startColumn = parcel.readInt()
            endLine = parcel.readInt()
            endColumn = parcel.readInt()
            text = parcel.readString()
        }

        override fun undo(content: Content) {
            content.delete(startLine, startColumn, endLine, endColumn)
        }

        override fun redo(content: Content) {
            content.insert(startLine, startColumn, text!!)
        }

        override fun canMerge(action: ContentAction): Boolean {
            if (action is InsertAction) {
                return (action.startColumn == endColumn && action.startLine == endLine
                        && (action.text?.length ?: 0) + (text?.length ?: 0) < 10000
                        && Math.abs(action.createTime - createTime) < sMergeTimeLimit)
            }
            return false
        }

        override fun merge(action: ContentAction) {
            if (!canMerge(action)) throw IllegalArgumentException()
            val ac = action as InsertAction
            endColumn = ac.endColumn
            endLine = ac.endLine
            val sb: StringBuilder = if (text is StringBuilder) {
                text as StringBuilder
            } else {
                StringBuilder(text!!).also { text = it }
            }
            sb.append(ac.text)
        }

        override fun toString(): String {
            return "InsertAction(startLine=$startLine, endLine=$endLine, startColumn=$startColumn, endColumn=$endColumn, createTime=$createTime, text=$text)"
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(startLine)
            parcel.writeInt(startColumn)
            parcel.writeInt(endLine)
            parcel.writeInt(endColumn)
            parcel.writeString(text?.toString())
        }

        companion object CREATOR : Parcelable.Creator<InsertAction> {
            override fun createFromParcel(parcel: Parcel): InsertAction = InsertAction(parcel)
            override fun newArray(size: Int): Array<InsertAction?> = arrayOfNulls(size)
        }
    }

    /**
     * MultiAction saves several actions for UndoManager
     */
    class MultiAction : ContentAction {
        val actions: MutableList<ContentAction> = ArrayList()

        constructor()

        private constructor(parcel: Parcel) {
            val count = parcel.readInt()
            repeat(count) {
                actions.add(parcel.readParcelable<ContentAction>(MultiAction::class.java.classLoader)!!)
            }
        }

        fun addAction(action: ContentAction) {
            if (actions.isEmpty()) {
                actions.add(action)
            } else {
                val last = actions.last()
                if (last.canMerge(action)) {
                    last.merge(action)
                } else {
                    actions.add(action)
                }
            }
        }

        override fun undo(content: Content) {
            for (i in actions.size - 1 downTo 0) {
                actions[i].undo(content)
            }
        }

        override fun redo(content: Content) {
            for (i in actions.indices) {
                actions[i].redo(content)
            }
        }

        override fun canMerge(action: ContentAction): Boolean = false
        override fun merge(action: ContentAction) = throw UnsupportedOperationException()
        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(actions.size)
            for (action in actions) {
                parcel.writeParcelable(action, flags)
            }
        }

        companion object CREATOR : Parcelable.Creator<MultiAction> {
            override fun createFromParcel(parcel: Parcel): MultiAction = MultiAction(parcel)
            override fun newArray(size: Int): Array<MultiAction?> = arrayOfNulls(size)
        }
    }

    /**
     * Delete action model for UndoManager
     */
    class DeleteAction : ContentAction {
        @JvmField
        var startLine: Int = 0
        @JvmField
        var endLine: Int = 0
        @JvmField
        var startColumn: Int = 0
        @JvmField
        var endColumn: Int = 0
        @JvmField
        @Transient
        var createTime: Long = System.currentTimeMillis()
        @JvmField
        var text: CharSequence? = null

        constructor()

        private constructor(parcel: Parcel) {
            startLine = parcel.readInt()
            startColumn = parcel.readInt()
            endLine = parcel.readInt()
            endColumn = parcel.readInt()
            text = parcel.readString()
        }

        override fun undo(content: Content) {
            content.insert(startLine, startColumn, text!!)
        }

        override fun redo(content: Content) {
            content.delete(startLine, startColumn, endLine, endColumn)
        }

        override fun canMerge(action: ContentAction): Boolean {
            if (action is DeleteAction) {
                return (action.endColumn == startColumn && action.endLine == startLine
                        && (action.text?.length ?: 0) + (text?.length ?: 0) < 10000
                        && Math.abs(action.createTime - createTime) < sMergeTimeLimit)
            }
            return false
        }

        override fun merge(action: ContentAction) {
            if (!canMerge(action)) throw IllegalArgumentException()
            val ac = action as DeleteAction
            startColumn = ac.startColumn
            startLine = ac.startLine
            val sb: StringBuilder = if (text is StringBuilder) {
                text as StringBuilder
            } else {
                StringBuilder(text!!).also { text = it }
            }
            sb.insert(0, ac.text)
        }

        override fun toString(): String {
            return "DeleteAction(startLine=$startLine, endLine=$endLine, startColumn=$startColumn, endColumn=$endColumn, createTime=$createTime, text=$text)"
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(startLine)
            parcel.writeInt(startColumn)
            parcel.writeInt(endLine)
            parcel.writeInt(endColumn)
            parcel.writeString(text?.toString())
        }

        companion object CREATOR : Parcelable.Creator<DeleteAction> {
            override fun createFromParcel(parcel: Parcel): DeleteAction = DeleteAction(parcel)
            override fun newArray(size: Int): Array<DeleteAction?> = arrayOfNulls(size)
        }
    }

    /**
     * Replace action model for UndoManager
     */
    class ReplaceAction : ContentAction {
        @JvmField
        var insert: InsertAction? = null
        @JvmField
        var delete: DeleteAction? = null

        constructor()

        private constructor(parcel: Parcel) {
            insert = parcel.readParcelable(ReplaceAction::class.java.classLoader)
            delete = parcel.readParcelable(ReplaceAction::class.java.classLoader)
        }

        override fun undo(content: Content) {
            insert?.undo(content)
            delete?.undo(content)
        }

        override fun redo(content: Content) {
            delete?.redo(content)
            insert?.redo(content)
        }

        override fun canMerge(action: ContentAction): Boolean = false
        override fun merge(action: ContentAction) = throw UnsupportedOperationException()

        override fun toString(): String = "ReplaceAction(insert=$insert, delete=$delete)"

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(insert, flags)
            parcel.writeParcelable(delete, flags)
        }

        companion object CREATOR : Parcelable.Creator<ReplaceAction> {
            override fun createFromParcel(parcel: Parcel): ReplaceAction = ReplaceAction(parcel)
            override fun newArray(size: Int): Array<ReplaceAction?> = arrayOfNulls(size)
        }
    }

    companion object {
        @JvmStatic
        var sMergeTimeLimit: Long = 8000L

        @JvmField
        val CREATOR: Parcelable.Creator<UndoManager> = object : Parcelable.Creator<UndoManager> {
            override fun createFromParcel(parcel: Parcel): UndoManager = UndoManager(parcel)
            override fun newArray(size: Int): Array<UndoManager?> = arrayOfNulls(size)
        }
    }
}
