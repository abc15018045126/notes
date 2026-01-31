package io.github.abc15018045126.sora.util

import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

open class SegmentList<T>(private val segmentCapacity: Int = DEFAULT_SEGMENT_CAPACITY) : AbstractMutableList<T>() {

    companion object {
        const val DEFAULT_SEGMENT_CAPACITY = 8192
    }

    private val segments = ArrayList<Segment<T>>()
    private var _size: Int = 0

    override val size: Int
        get() = _size

    init {
        if (segmentCapacity < 4) {
            throw IllegalArgumentException("block size should be at least 4")
        }
    }

    private fun checkInsertIndex(index: Int) {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException("index $index out of bounds. length = $size")
        }
    }

    private fun checkAccessIndex(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index $index out of bounds. length = $size")
        }
    }

    private class FindResult<T>(
        var segment: Segment<T>? = null,
        var offset: Int = 0,
        var blockIndex: Int = 0
    ) {
        fun set(segment: Segment<T>, offset: Int, segIndex: Int): FindResult<T> {
            this.segment = segment
            this.offset = offset
            this.blockIndex = segIndex
            return this
        }
    }

    private val result = FindResult<T>()

    private fun makeResult(segment: Segment<T>, offset: Int, segIndex: Int): FindResult<T> {
        return result.set(segment, offset, segIndex)
    }

    private fun getSegment(index: Int): FindResult<T> {
        if (segments.isEmpty()) {
            segments.add(Segment(segmentCapacity))
        }
        var offset = 0
        var backBlock = segments[segments.size - 1]
        var backOffset = size - backBlock.size
        
        // Iterating from both ends
        var i = 0
        var j = segments.size - 1
        while (i <= j) {
            var block = segments[i]
            if ((index >= offset && index < offset + block.size) || i + 1 == segments.size) {
                return makeResult(block, offset, i)
            }
            offset += block.size

            block = backBlock
            if ((index >= backOffset && index < backOffset + block.size) || (j == segments.size - 1 && index == size)) {
                return makeResult(block, backOffset, j)
            }
            if (j > 0) {
                backBlock = segments[j - 1]
                backOffset -= backBlock.size
            }
            i++
            j--
        }
        throw IllegalStateException("unreachable")
    }

    private fun getSegmentMut(index: Int): FindResult<T> {
        val res = getSegment(index)
        res.segment = ensureMutable(res.blockIndex)
        return res
    }

    private fun ensureMutable(segIdx: Int): Segment<T> {
        val block = segments[segIdx]
        val n = block.toMutable()
        if (block !== n) {
            segments[segIdx] = n
            block.release()
            return n
        }
        return block
    }

    override fun set(index: Int, element: T): T {
        checkAccessIndex(index)
        val result = getSegmentMut(index)
        return result.segment!!.set(index - result.offset, element)
    }

    override fun add(index: Int, element: T) {
        checkInsertIndex(index)
        val result = getSegmentMut(index)
        result.segment!!.add(index - result.offset, element)
        _size++
        adjustElements(result.blockIndex, result.segment!!)
        if (result.segment!!.size >= segmentCapacity) {
            val divPoint = segmentCapacity / 2
            val seg = Segment<T>(segmentCapacity)
            val sub = result.segment!!.subList(divPoint, result.segment!!.size)
            seg.addAll(sub)
            sub.clear()
            segments.add(result.blockIndex + 1, seg)
        }
    }

    override fun removeAt(index: Int): T {
        checkAccessIndex(index)
        val result = getSegmentMut(index)
        val res = result.segment!!.removeAt(index - result.offset)
        _size--
        mergeSegment(index - 1, index)
        mergeSegment(index, index + 1)
        return res
    }

    override fun get(index: Int): T {
        checkAccessIndex(index)
        val result = getSegment(index)
        return result.segment!![index - result.offset]
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        if (fromIndex > toIndex) throw IndexOutOfBoundsException("start > end")
        if (fromIndex < 0 || toIndex > size)
            throw IndexOutOfBoundsException("start = $fromIndex, end = $toIndex, length = $size")
        if (fromIndex == toIndex) return
        val res = getSegment(fromIndex)
        var offset = res.offset
        var index = res.blockIndex
        var seg = res.segment!!

        while (toIndex - offset > 0 && index < segments.size) {
            val segLength = seg.size
            if (fromIndex <= offset && toIndex >= offset + segLength) {
                // Remove the segment
                segments.removeAt(index)
                seg.release()
            } else {
                ensureMutable(index)
                seg = segments[index]
                val sub = seg.subList(max(fromIndex - offset, 0), min(toIndex - offset, segLength))
                sub.clear()
                index++
            }
            offset += segLength
            if (index < segments.size)
                seg = segments[index]
        }
        mergeSegment(index - 1, index)
        _size -= toIndex - fromIndex
    }

    override fun clear() {
        for (seg in segments) {
            seg.release()
        }
        segments.clear()
        _size = 0
    }

    fun shallowCopy(): SegmentList<T> {
        val list = SegmentList<T>(segmentCapacity)
        list.segments.clear()
        for (seg in segments) {
            seg.retain()
        }
        list.segments.addAll(segments)
        list._size = _size
        return list
    }

    private fun mergeSegment(seg1: Int, seg2: Int) {
        var s1 = seg1
        var s2 = seg2
        if (s1 > s2) {
            val tmp = s1
            s1 = s2
            s2 = tmp
        }
        if (s1 == s2 || s1 < 0 || s2 >= segments.size) return
        var pre = segments[s1]
        val aft = segments[s2]
        if (pre.size + aft.size <= segmentCapacity * 3 / 4) {
            ensureMutable(s1)
            pre = segments[s1]
            pre.addAll(aft)
            segments.removeAt(s2)
            aft.release()
        }
    }

    private fun adjustElements(segIdx: Int, mutCur: Segment<T>) {
        if (segIdx > 0) {
            val pre = segments[segIdx - 1]
            if (pre.isMutable() && pre.size <= segmentCapacity * 4 / 5 && mutCur.size > segmentCapacity * 4 / 5) {
                val sub = mutCur.subList(0, segmentCapacity * 4 / 5 - pre.size)
                pre.addAll(sub)
                sub.clear()
            }
        }
    }

    fun forEachCompat(consumer: ConsumerCompat<T>) {
        for (i in segments.indices) {
            val seg = segments[i]
            for (i1 in 0 until seg.size) {
                consumer.accept(seg[i1])
            }
        }
    }

    fun interface ConsumerCompat<T> {
        fun accept(obj: T)
    }

    private class Segment<T> : ArrayList<T>, ShareableData<Segment<T>> {

        constructor() : super()
        constructor(initialCapacity: Int) : super(initialCapacity)

        private val refCount = AtomicInteger(1)

        override fun retain() {
            refCount.incrementAndGet()
        }

        override fun release() {
            if (refCount.decrementAndGet() < 0) {
                throw IllegalStateException("illegal release invocation")
            }
        }

        override fun isMutable(): Boolean {
            return refCount.get() == 1
        }

        override fun toMutable(): Segment<T> {
            return if (isMutable()) {
                this
            } else {
                copy()
            }
        }

        fun copy(): Segment<T> {
            val res = Segment<T>(this.size)
            res.addAll(this)
            return res
        }
    }
}
