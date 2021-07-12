package com.tompee.bunch.compiler.extensions

/**
 * Replaces all whitespace with wrap-proof whitespaces
 */
internal fun String.wrapProof() : String {
    return this.replace(" ", "·")
}

/**
 * Capitalizes the first letter of a string
 */
internal fun String.capitalize() : String {
    return replaceFirstChar { it.uppercase() }
}