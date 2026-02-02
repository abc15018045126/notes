package io.github.abc15018045126.sora.lang.brackets

/**
 * Describes paired brackets
 *
 * @author abc15018045126
 */
class PairedBracket @JvmOverloads constructor(
    @JvmField val leftIndex: Int,
    @JvmField val leftLength: Int = 1,
    @JvmField val rightIndex: Int,
    @JvmField val rightLength: Int = 1
)
