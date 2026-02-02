
package io.github.abc15018045126.sora.widget.component

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.event.CreateContextMenuEvent
import io.github.abc15018045126.sora.event.subscribeAlways
import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Add context menu items for editor
 *
 * @author abc15018045126
 */
open class EditorContextMenuCreator(val editor: CodeEditor) : EditorBuiltinComponent {

    val eventManager = editor.createSubEventManager()

    init {
        eventManager.subscribeAlways(::onCreateContextMenu)
    }

    open fun onCreateContextMenu(event: CreateContextMenuEvent) {
        buildMenu(event.menu) {

            item {
                titleRes = android.R.string.selectAll
                iconRes = R.drawable.round_select_all_20
                isEnabled = !editor.text.isEmpty()
                onClick {
                    editor.selectAll()
                }
            }

            item {
                titleRes = android.R.string.copy
                iconRes = R.drawable.round_content_copy_20
                isEnabled = editor.isTextSelected
                onClick {
                    editor.copyText()
                }
            }

            item {
                titleRes = android.R.string.cut
                iconRes = R.drawable.round_content_cut_20
                isEnabled = editor.isTextSelected
                onClick {
                    editor.cutText()
                }
            }

            item {
                titleRes = android.R.string.paste
                iconRes = R.drawable.round_content_paste_20
                isEnabled = editor.hasClip()
                onClick {
                    editor.pasteText()
                }
            }

        }
    }

    override var isEnabled: Boolean
        get() = eventManager.isEnabled
        set(value) {
            eventManager.isEnabled = value
        }

    @DslMarker
    annotation class MenuDslMarker

    @MenuDslMarker
    open class MenuBuilder(val context: Context, val menu: Menu) {

        private val items = mutableListOf<MenuItemBuilder>()

        fun item(builder: MenuItemBuilder.() -> Unit) {
            items.add(MenuItemBuilder(context).also { it.builder() })
        }

        fun subMenu(builder: SubMenuBuilder.() -> Unit) {
            items.add(SubMenuBuilder(context).also { it.builder() })
        }

        internal open fun build() {
            items.forEach {
                it.build(menu)
            }
        }

    }

    @MenuDslMarker
    open class ContextMenuBuilder(context: Context, val contextMenu: ContextMenu) :
        MenuBuilder(context, contextMenu) {

        var headerTitle: CharSequence? = null

        var headerTitleRes: Int = 0
            set(value) {
                headerTitle = context.getString(value)
            }

        override fun build() {
            super.build()
            if (headerTitle != null)
                contextMenu.setHeaderTitle(headerTitle)
        }

    }

    @MenuDslMarker
    open class SubMenuBuilder(context: Context) : MenuItemBuilder(context) {

        var headerTitle: CharSequence? = null

        var headerTitleRes: Int = 0
            set(value) {
                headerTitle = context.getString(value)
            }

        private val items = mutableListOf<MenuItemBuilder>()

        fun item(builder: MenuItemBuilder.() -> Unit) {
            items.add(MenuItemBuilder(context).also { it.builder() })
        }

        fun subMenu(builder: SubMenuBuilder.() -> Unit) {
            items.add(SubMenuBuilder(context).also { it.builder() })
        }

        override fun build(menu: Menu) {
            val subMenu = menu.addSubMenu(groupId, itemId, order, title)
                .also {
                    if (iconRes != 0) {
                        it.setIcon(iconRes)
                    } else if (icon != null) {
                        it.setIcon(icon)
                    }
                }
            headerTitle?.let {
                subMenu.setHeaderTitle(it)
            }
            items.forEach {
                it.build(subMenu)
            }
        }

    }

    @MenuDslMarker
    open class MenuItemBuilder(val context: Context) {
        var groupId = 0
        var itemId = 0
        var order = 0
        var title = ""
        var isEnabled = true
        var icon: Drawable? = null
        var iconRes: Int = 0
        var titleRes: Int = 0
            set(value) {
                title = context.getString(value)
            }
        var onClick: MenuItem.OnMenuItemClickListener? = null

        fun onClick(listener: () -> Unit) {
            onClick = MenuItem.OnMenuItemClickListener {
                listener()
                true
            }
        }

        internal open fun build(menu: Menu) {
            menu.add(groupId, itemId, order, title)
                .setEnabled(isEnabled)
                .setOnMenuItemClickListener(onClick).also {
                    if (iconRes != 0) {
                        it.setIcon(iconRes)
                    } else if (icon != null) {
                        it.icon = icon
                    }
                }
        }
    }

    fun buildMenu(menu: ContextMenu, builder: ContextMenuBuilder.() -> Unit) {
        ContextMenuBuilder(editor.context, menu).also {
            it.builder()
        }.build()
    }

}

