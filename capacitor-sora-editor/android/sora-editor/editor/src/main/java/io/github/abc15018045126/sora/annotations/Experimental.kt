package io.github.abc15018045126.sora.annotations

/**
 * This annotation marks those fields, methods and constructors experimentally created.
 * <p>
 * Methods, fields and constructors with this annotation is very subject to keep or delete.
 * For that reason, they are not stable for production use.
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.SOURCE)
annotation class Experimental
