package io.github.abc15018045126.sora.util

import kotlin.math.abs
import kotlin.math.max

/**
 * @author Rose
 * TrieTree to query values quickly
 */
class TrieTree<T> {

    @JvmField
    val root: Node<T> = Node()
    private var maxLen = 0

    fun put(v: String, token: T) {
        maxLen = max(v.length, maxLen)
        addInternal(root, v, 0, v.length, token)
    }

    fun put(v: CharSequence, off: Int, len: Int, token: T) {
        maxLen = max(maxLen, len)
        addInternal(root, v, off, len, token)
    }

    operator fun get(s: CharSequence, offset: Int, len: Int): T? {
        if (len > maxLen) {
            return null
        }
        return getInternal(root, s, offset, len)
    }

    private fun getInternal(node: Node<T>, s: CharSequence, offset: Int, len: Int): T? {
        if (len == 0) {
            return node.token
        }
        val point = s[offset]
        val sub = node.map.get(point) ?: return null
        return getInternal(sub, s, offset + 1, len - 1)
    }

    private fun addInternal(node: Node<T>, v: CharSequence, i: Int, len: Int, token: T) {
        val point = v[i]
        var sub = node.map.get(point)
        if (sub == null) {
            sub = Node()
            node.map.put(point, sub)
        }
        if (len == 1) {
            sub.token = token
        } else {
            addInternal(sub, v, i + 1, len - 1, token)
        }
    }

    class Node<T> {

        @JvmField
        val map: HashCharMap<Node<T>> = HashCharMap()

        @JvmField
        var token: T? = null
    }

    /**
     * Hashmap with fixed length
     *
     * @author abc15018045126
     */
    class HashCharMap<V> {

        private val columns: Array<LinkedPair<V>?> = arrayOfNulls(CAPACITY)
        private val ends: Array<LinkedPair<V>?> = arrayOfNulls(CAPACITY)

        companion object {
            private const val CAPACITY = 64

            private fun position(first: Int): Int {
                return abs(first xor (first shl 6) * if (first and 1 != 0) 3 else 1) % CAPACITY
            }
        }

        fun get(first: Char): V? {
            val position = position(first.code)
            var pair = columns[position]
            while (pair != null) {
                if (pair.first == first) {
                    return pair.second
                }
                pair = pair.next
            }
            return null
        }

        private fun get(first: Char, position: Int): LinkedPair<V>? {
            var pair = columns[position]
            while (pair != null) {
                if (pair.first == first) {
                    return pair
                }
                pair = pair.next
            }
            return null
        }

        fun put(first: Char, second: V) {
            val position = position(first.code)
            if (ends[position] == null) {
                val pair = LinkedPair<V>()
                ends[position] = pair
                columns[position] = pair
                pair.first = first
                pair.second = second
                return
            }
            var p = get(first, position)
            if (p == null) {
                p = LinkedPair()
                ends[position]!!.next = p
                ends[position] = p
            }
            p.first = first
            p.second = second
        }
    }

    /**
     * 数据节点
     *
     * @author Rose
     */
    class LinkedPair<V> {
        @JvmField var next: LinkedPair<V>? = null
        @JvmField var first: Char = '\u0000'
        @JvmField var second: V? = null
    }
}
