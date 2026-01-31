package io.github.abc15018045126.sora.util

import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max
import kotlin.math.min

class BlockIntList(private val blockSize: Int = 1000) {

    companion object {
        private const val CACHE_COUNT = 8
        private const val CACHE_SWITCH = 30
    }

    @JvmField
    val lock: Lock = ReentrantLock()
    private val recycled = ArrayList<Block>()
    private val caches = ArrayList<Cache>(CACHE_COUNT + 2)
    var size: Int = 0
        private set
    private var modCount: Int = 0
    private var head: Block = Block()
    private var foundIndex: Int = 0
    private var foundBlock: Block? = null
    private var updateTime: Int = 0
    var max: Int = 0
        get() {
            if (modCount != updateTime) {
                updateTime = modCount
                computeMax()
            }
            return field
        }
        private set

    init {
        if (blockSize <= 4) {
            throw IllegalArgumentException("block size must be bigger than 4")
        }
    }

    private fun computeMax() {
        var m = 0
        var block: Block? = head
        while (block != null) {
            m = max(m, block.max)
            block = block.next
        }
        max = m
    }

    private fun findBlock1(index: Int) {
        var distance = index
        var usedNo = -1
        var fromBlock: Block = head
        for (i in caches.indices) {
            val c = caches[i]
            if (c.indexOfStart < index && (index - c.indexOfStart) < distance) {
                distance = index - c.indexOfStart
                fromBlock = c.block!!
                usedNo = i
            }
        }
        if (usedNo != -1) {
            Collections.swap(caches, 0, usedNo)
        }
        var crossCount = 0
        while (distance >= fromBlock.size) {
            if (fromBlock.next != null) {
                distance -= fromBlock.size
                fromBlock = fromBlock.next!!
            } else {
                break
            }
            crossCount++
        }
        if (crossCount >= CACHE_SWITCH) {
            caches.add(cache(index - distance, fromBlock))
        }
        if (caches.size > CACHE_COUNT) {
            caches.removeAt(caches.size - 1)
        }
        foundIndex = distance
        foundBlock = fromBlock
    }

    private fun invalidateCacheFrom(index: Int) {
        var i = 0
        while (i < caches.size) {
            if (caches[i].indexOfStart >= index) {
                caches.removeAt(i)
                i--
            }
            i++
        }
    }

    private fun newBlock(): Block {
        if (recycled.isEmpty()) {
            return Block()
        }
        return recycled.removeAt(recycled.size - 1)
    }

    fun add(element: Int) {
        add(size, element)
    }

    fun add(index: Int, element: Int) {
        var idx = index
        if (idx < 0 || idx > size) {
            throw ArrayIndexOutOfBoundsException("index = $idx, length = $size")
        }
        findBlock1(idx)
        invalidateCacheFrom(idx)
        var block = foundBlock!!
        idx = foundIndex
        while (idx > block.size) {
            if (block.next == null) {
                break
            } else {
                idx -= block.size
                block = block.next!!
            }
        }
        block.add(idx, element)
        size++
        if (block.size > blockSize) {
            block.separate()
        }
        modCount++
    }

    fun remove(index: Int): Int {
        var idx = index
        if (idx < 0 || idx >= size) {
            throw ArrayIndexOutOfBoundsException("index = $idx, length = $size")
        }
        val backup = idx
        var previous: Block? = null
        var block: Block? = head
        while (idx >= block!!.size) {
            idx -= block.size
            previous = block
            block = block.next
        }
        val removedValue = block.remove(idx)
        invalidateCacheFrom(backup - idx)
        if (block.size == 0 && previous != null) {
            previous.next = block.next
            recycled.add(block)
        } else if (block.size < blockSize / 4 && previous != null && previous.size + block.size < blockSize / 2) {
            previous.next = block.next
            System.arraycopy(block.data, 0, previous.data, previous.size, block.size)
            previous.size += block.size
        }
        modCount++
        size--
        return removedValue
    }

    operator fun set(index: Int, element: Int): Int {
        if (index < 0 || index >= size) {
            throw ArrayIndexOutOfBoundsException("index = $index, length = $size")
        }
        findBlock1(index)
        val old = foundBlock!!.set(foundIndex, element)
        modCount++
        return old
    }

    operator fun get(index: Int): Int {
        if (index < 0 || index >= size) {
            throw ArrayIndexOutOfBoundsException("index = $index, length = $size")
        }
        findBlock1(index)
        return foundBlock!![foundIndex]
    }

    fun removeRange(fromIndex: Int, toIndex: Int) {
        var fromIdx = fromIndex
        var toIdx = toIndex
        if (toIdx > size || fromIdx < 0 || fromIdx > toIdx) {
            throw IndexOutOfBoundsException()
        }
        var previous: Block? = null
        var block: Block? = head
        while (fromIdx >= block!!.size) {
            fromIdx -= block.size
            toIdx -= block.size
            previous = block
            block = block.next
        }
        var deleteLength = toIdx - fromIdx
        var begin = fromIdx
        while (deleteLength > 0) {
            if (begin == 0 && deleteLength >= block!!.size) {
                if (previous != null) {
                    previous.next = block.next
                    recycled.add(block)
                }
                deleteLength -= block.size
                block.size = 0
                block = block.next
                continue
            }
            begin = 0
            val end = min(block!!.size, begin + deleteLength)
            block.remove(begin, end)
            deleteLength -= (end - begin)
            previous = block
            block = block.next
        }
        size -= (toIndex - fromIndex)
    }

    fun clear() {
        head = Block()
        size = 0
        caches.clear()
        foundBlock = null
        foundIndex = 0
    }

    fun size(): Int {
        return size
    }

    private fun cache(index: Int, block: Block): Cache {
        val c = Cache()
        c.indexOfStart = index
        c.block = block
        return c
    }

    private inner class Block {
        val data: IntArray = IntArray(blockSize + 5)
        var size: Int = 0
        var max: Int = 0
        var next: Block? = null

        fun add(index: Int, element: Int) {
            System.arraycopy(data, index, data, index + 1, size - index)
            data[index] = element
            size++
            if (element > max) {
                max = element
            }
        }

        operator fun set(index: Int, element: Int): Int {
            val old = data[index]
            data[index] = element
            if (old == max) {
                if (element >= old) {
                    max = element
                } else {
                    compute()
                }
            } else if (element > max) {
                max = element
            }
            return old
        }

        operator fun get(index: Int): Int {
            return data[index]
        }

        fun remove(index: Int): Int {
            val oldValue = data[index]
            System.arraycopy(data, index + 1, data, index, size - index - 1)
            size--
            if (oldValue == max) {
                compute()
            }
            return oldValue
        }

        fun remove(start: Int, end: Int) {
            System.arraycopy(data, end, data, start, size - end)
            size -= (end - start)
            compute()
        }

        fun separate() {
            val oldNext = this.next
            val newNext = newBlock()
            val divPoint = blockSize * 3 / 4
            System.arraycopy(this.data, divPoint, newNext.data, 0, this.size - divPoint)
            newNext.size = this.size - divPoint
            this.size = divPoint
            this.next = newNext
            newNext.next = oldNext
            compute()
            newNext.compute()
        }

        private fun compute() {
            var m = 0
            for (i in 0 until size) {
                m = max(m, data[i])
            }
            max = m
        }
    }

    private class Cache {
        var block: Block? = null
        var indexOfStart: Int = 0
    }
}
