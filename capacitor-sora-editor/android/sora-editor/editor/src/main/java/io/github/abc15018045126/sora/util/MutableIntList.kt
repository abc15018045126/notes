package io.github.abc15018045126.sora.util

import java.util.ArrayList

class MutableIntList {
    private val data = ArrayList<Int>()

    fun add(value: Int) {
        data.add(value)
    }

    fun get(index: Int): Int {
        return data[index]
    }

    fun size(): Int {
        return data.size
    }
    
    val size: Int
        get() = data.size

    fun clear() {
        data.clear()
    }
    
    fun set(index: Int, value: Int) {
        if (index >= data.size) {
             for (i in data.size..index) {
                 data.add(0)
             }
        }
        data[index] = value
    }
}
