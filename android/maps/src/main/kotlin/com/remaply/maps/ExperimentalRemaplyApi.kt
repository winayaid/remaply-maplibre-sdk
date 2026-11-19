package com.remaply.maps

/**
 * Marks APIs that are experimental: not covered by semantic versioning or by platform parity.
 *
 * Opt in with `@OptIn(ExperimentalRemaplyApi::class)`.
 */
@RequiresOptIn(
    message = "This Remaply Maps API is experimental and may change without notice.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class ExperimentalRemaplyApi
