package io.github.abc15018045126.sora.util

import android.util.Log
import java.util.WeakHashMap

class Logger private constructor(private val name: String) {

    fun d(msg: String) {
        Log.d(name, msg)
    }

    fun d(msg: String, vararg format: Any) {
        Log.d(name, String.format(msg, *format))
    }

    fun i(msg: String) {
        Log.i(name, msg)
    }

    fun i(msg: String, vararg format: Any) {
        Log.i(name, String.format(msg, *format))
    }

    fun v(msg: String) {
        Log.v(name, msg)
    }

    fun v(msg: String, vararg format: Any) {
        Log.v(name, String.format(msg, *format))
    }

    fun w(msg: String) {
        Log.w(name, msg)
    }

    fun w(msg: String, vararg format: Any) {
        Log.w(name, String.format(msg, *format))
    }

    fun w(msg: String, e: Throwable) {
        Log.w(name, msg, e)
    }

    fun w(msg: String, e: Throwable, vararg format: Any) {
        Log.w(name, String.format(msg, *format), e)
    }

    fun e(msg: String) {
        Log.e(name, msg)
    }

    fun e(msg: String, vararg format: Any) {
        Log.e(name, String.format(msg, *format))
    }

    fun e(msg: String, e: Throwable) {
        Log.e(name, msg, e)
    }

    fun e(msg: String, e: Throwable, vararg format: Any) {
        Log.e(name, String.format(msg, *format), e)
    }

    companion object {
        private val map: MutableMap<String, Logger> = WeakHashMap()

        @JvmStatic
        @Synchronized
        fun instance(name: String): Logger {
            var logger = map[name]
            if (logger == null) {
                logger = Logger(name)
                map[name] = logger
            }
            return logger!!
        }
    }
}
