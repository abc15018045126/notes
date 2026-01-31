package io.github.abc15018045126.sora.util

abstract class ObjectPool<T>(size: Int = 16) {

    private val pool: Array<Any?> = arrayOfNulls(size)

    open fun recycle(obj: T?) {
        if (obj == null) {
            return
        }
        onRecycleObject(obj)
        synchronized(this) {
            for (i in pool.indices) {
                if (pool[i] == null) {
                    pool[i] = obj
                    break
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun obtain(): T {
        var result: T? = null
        synchronized(this) {
            for (i in pool.indices.reversed()) {
                if (pool[i] != null) {
                    result = pool[i] as T
                    pool[i] = null
                    break
                }
            }
        }
        if (result == null) {
            result = allocateNew()
        }
        return result!!
    }

    protected open fun onRecycleObject(recycledObj: T) {
    }

    protected abstract fun allocateNew(): T
}
