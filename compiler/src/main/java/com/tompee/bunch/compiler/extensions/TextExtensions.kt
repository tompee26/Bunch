package com.tompee.bunch.compiler.extensions

/**
 * Replaces all whitespace with wrap-proof whitespaces
 */
internal fun String.wrapProof() : String {
    return this.replace(" ", "·")
}