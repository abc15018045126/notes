package io.github.abc15018045126.sora.util

import android.util.SparseIntArray
import io.github.abc15018045126.sora.util.IntPair.getFirst
import io.github.abc15018045126.sora.util.IntPair.getSecond
import io.github.abc15018045126.sora.util.IntPair.pack
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * A general implementation of big root heap
 *
 * @author abc15018045126
 */
class BinaryHeap {

    /**
     * Lock for multi-thread reusing
     */
    @JvmField
    val lock: Lock = ReentrantLock()

    /**
     * Map from id to its position in heap array
     */
    private val idToPosition: SparseIntArray = SparseIntArray()

    /**
     * Id allocator
     */
    private var idAllocator = 1

    /**
     * Current node count in heap
     */
    private var nodeCount = 0

    /**
     * Node array for heap.
     * first:  id
     * second: data
     */
    private var nodes: LongArray = LongArray(129)

    private fun id(value: Long): Int {
        return getFirst(value)
    }

    private fun data(value: Long): Int {
        return getSecond(value)
    }

    /**
     * Clear all the nodes in the heap
     */
    fun clear() {
        nodeCount = 0
        idToPosition.clear()
        idAllocator = 1
    }

    /**
     * Ensure there is enough space
     *
     * @param capacity desired space size
     */
    fun ensureCapacity(capacity: Int) {
        var cap = capacity
        cap++
        if (nodes.size < cap) {
            val origin = nodes
            if (nodes.size shl 1 >= cap) {
                nodes = LongArray(nodes.size shl 1)
            } else {
                nodes = LongArray(cap)
            }
            System.arraycopy(origin, 0, nodes, 0, nodeCount + 1)
        }
    }

    /**
     * Get the max value in this heap or zero if no node is in heap
     *
     * @return Max value
     */
    fun top(): Int {
        if (nodeCount == 0) {
            return 0
        }
        return data(nodes[1])
    }

    /**
     * Get total node count in heap
     */
    fun getNodeCount(): Int {
        return nodeCount
    }

    /**
     * Internal implementation to move down nodes
     *
     * @param position target node's position in heap
     */
    private fun heapifyDown(position: Int) {
        var pos = position
        var child = pos * 2
        while (child <= nodeCount) {
            val parentNode = nodes[pos]
            var childNode: Long
            if (child + 1 <= nodeCount && data(nodes[child + 1]) > data(nodes[child])) {
                child = child + 1
            }
            childNode = nodes[child]
            if (data(parentNode) < data(childNode)) {
                idToPosition.put(id(childNode), pos)
                idToPosition.put(id(parentNode), child)
                nodes[child] = parentNode
                nodes[pos] = childNode
                pos = child
            } else {
                break
            }
            child = pos * 2
        }
    }

    /**
     * Internal implementation to move up nodes
     *
     * @param position target node's position in heap
     */
    private fun heapifyUp(position: Int) {
        var pos = position
        var parent = pos / 2
        while (parent >= 1) {
            val childNode = nodes[pos]
            val parentNode = nodes[parent]
            if (data(childNode) > data(parentNode)) {
                idToPosition.put(id(childNode), parent)
                idToPosition.put(id(parentNode), pos)
                nodes[pos] = parentNode
                nodes[parent] = childNode
                pos = parent
            } else {
                break
            }
            parent = pos / 2
        }
    }

    /**
     * Add a new node to the heap
     *
     * @return ID of node
     * @throws IllegalStateException when there is no new id available
     */
    fun push(value: Int): Int {
        ensureCapacity(nodeCount + 1)
        if (idAllocator == Int.MAX_VALUE) {
            throw IllegalStateException("unable to allocate more id")
        }
        val id = idAllocator++
        nodeCount++
        nodes[nodeCount] = pack(id, value)
        idToPosition.put(id, nodeCount)
        heapifyUp(nodeCount)
        return id
    }

    /**
     * Update the value of node with given id to newValue
     *
     * @param id       ID returned by push()
     * @param newValue new value for this node
     * @throws IllegalArgumentException when the id is invalid
     */
    fun update(id: Int, newValue: Int) {
        val position = idToPosition.get(id, 0)
        if (position == 0) {
            throw IllegalArgumentException("trying to update with an invalid id")
        }
        val origin = data(nodes[position])
        nodes[position] = pack(id(nodes[position]), newValue)
        if (origin < newValue) {
            heapifyUp(position)
        } else if (origin > newValue) {
            heapifyDown(position)
        }
    }

    /**
     * Remove node with given id
     *
     * @param id ID returned by push()
     * @throws IllegalArgumentException when the id is invalid
     */
    fun remove(id: Int) {
        val position = idToPosition.get(id, 0)
        if (position == 0) {
            throw IllegalArgumentException("trying to remove with an invalid id")
        }
        idToPosition.delete(id)
        //Replace removed node with last node
        nodes[position] = nodes[nodeCount]
        //Release node
        nodes[nodeCount--] = 0
        //Do not update heap if it is just the last node
        if (position == nodeCount + 1) {
            return
        }
        idToPosition.put(id(nodes[position]), position)
        heapifyUp(position)
        heapifyDown(position)
    }
}
