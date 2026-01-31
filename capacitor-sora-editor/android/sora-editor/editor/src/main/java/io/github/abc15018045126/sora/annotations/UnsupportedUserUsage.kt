package io.github.abc15018045126.sora.annotations

/**
 * Marks that this member is internally used and that it is not recommended using this
 * member at your side.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.CLASS
)
annotation class UnsupportedUserUsage
