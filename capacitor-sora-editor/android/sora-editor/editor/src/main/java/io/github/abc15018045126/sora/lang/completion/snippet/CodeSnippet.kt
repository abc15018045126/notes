package io.github.abc15018045126.sora.lang.completion.snippet

import android.text.TextUtils
import java.util.Objects
import java.util.TreeSet

class CodeSnippet(
    val items: MutableList<SnippetItem>,
    val placeholderDefinitions: MutableList<PlaceholderDefinition>
) : Cloneable {

    fun checkContent(): Boolean {
        var index = 0
        for (item in items) {
            if (item.startIndex != index) {
                return false
            }
            if (item is PlaceholderItem) {
                if (!placeholderDefinitions.contains(item.definition)) {
                    return false
                }
            }
            index = item.endIndex
        }
        val set = TreeSet<Int>()
        for (placeholder in placeholderDefinitions) {
            if (!set.contains(placeholder.id)) {
                set.add(placeholder.id)
            } else {
                return false
            }
        }
        return true
    }

    public override fun clone(): CodeSnippet {
        val defs = ArrayList<PlaceholderDefinition>(placeholderDefinitions.size)
        val map = HashMap<PlaceholderDefinition, PlaceholderDefinition>()
        for (placeholder in placeholderDefinitions) {
            val n = PlaceholderDefinition(
                placeholder.id,
                placeholder.choices,
                placeholder.elements,
                placeholder.transform
            )
            defs.add(n)
            map[placeholder] = n
        }
        val itemsClone = ArrayList<SnippetItem>(items.size)
        for (item in items) {
            val n = item.clone()
            itemsClone.add(n)
            if (n is PlaceholderItem) {
                if (map[n.definition] != null) {
                    n.definition = map[n.definition]!!
                }
            }
        }
        return CodeSnippet(itemsClone, defs)
    }

    class Builder(private val definitions: MutableList<PlaceholderDefinition> = ArrayList()) {
        private val items: MutableList<SnippetItem> = ArrayList()
        private var index: Int = 0

        fun addPlainText(text: String): Builder {
            if (items.isNotEmpty() && items[items.size - 1] is PlainTextItem) {
                // Merge plain texts
                val item = items[items.size - 1] as PlainTextItem
                item.text = item.text + text
                item.setIndex(item.startIndex, item.endIndex + text.length)
                index += text.length
                return this
            }
            items.add(PlainTextItem(text, index))
            index += text.length
            return this
        }

        fun addInterpolatedShell(shell: String): Builder {
            items.add(InterpolatedShellItem(shell, index))
            return this
        }

        fun addPlaceholder(id: Int): Builder {
            return addPlaceholder(id, null as String?)
        }

        fun addPlaceholder(id: Int, choices: List<String>): Builder {
            if (choices.isEmpty()) {
                return addPlaceholder(id)
            } else if (choices.size == 1) {
                return addPlaceholder(id, choices[0])
            }
            addPlaceholder(id, choices[0])
            var def: PlaceholderDefinition? = null
            for (definition in definitions) {
                if (definition.id == id) {
                    def = definition
                    break
                }
            }
            Objects.requireNonNull(def!!).choices = choices
            return this
        }

        fun addPlaceholder(id: Int, transform: Transform?): Builder {
            if (transform == null) {
                return addPlaceholder(id)
            }
            addPlaceholder(id)
            var def: PlaceholderDefinition? = null
            for (definition in definitions) {
                if (definition.id == id) {
                    def = definition
                    break
                }
            }
            Objects.requireNonNull(def!!).transform = transform
            return this
        }

        fun addPlaceholder(id: Int, defaultValue: String?): Builder {
            val elements = ArrayList<PlaceHolderElement>()
            if (!TextUtils.isEmpty(defaultValue)) {
                elements.add(PlainPlaceholderElement(defaultValue!!))
            }
            return addComplexPlaceholder(id, elements)
        }

        fun addComplexPlaceholder(id: Int, elements: List<PlaceHolderElement>): Builder {
            var def: PlaceholderDefinition? = null
            for (definition in definitions) {
                if (definition.id == id) {
                    def = definition
                    break
                }
            }
            if (def == null) {
                def = PlaceholderDefinition(id)
                definitions.add(def)
            }

            def!!.elements.addAll(elements)

            val item = PlaceholderItem(def, index)
            items.add(item)
            return this
        }

        fun addVariable(name: String, defaultValue: String?): Builder {
            items.add(VariableItem(index, name, defaultValue))
            return this
        }

        fun addVariable(name: String, transform: Transform?): Builder {
            items.add(VariableItem(index, name, null, transform))
            return this
        }

        fun addVariable(item: VariableItem): Builder {
            item.setIndex(index)
            items.add(item)
            return this
        }

        fun build(): CodeSnippet {
            return CodeSnippet(items, definitions)
        }
    }
}
