package io.github.abc15018045126.sora.lang.completion.snippet

import java.util.regex.Pattern

class Transform {

    @JvmField
    var regexp: Pattern? = null

    @JvmField
    var globalMode: Boolean = false

    @JvmField
    var format: List<@JvmSuppressWildcards FormatString>? = null
}
