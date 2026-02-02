package io.github.abc15018045126.sora.util

import java.util.HashMap

class MutableLongLongMap {
    private val data = HashMap<Long, Long>()

    fun put(key: Long, value: Long) {
        data[key] = value
    }

    fun get(key: Long): Long {
        return data[key] ?: 0L
    }

    fun clear() {
        data.clear()
    }
    
    fun containsKey(key: Long): Boolean {
        return data.containsKey(key)
    }

    val size: Int
        get() = data.size

    fun forEach(consumer: Consumer) {
        for ((key, value) in data) {
            consumer.accept(key, value)
        }
    }

    interface Consumer {
        fun accept(key: Long, value: Long): Any?
    }
}
