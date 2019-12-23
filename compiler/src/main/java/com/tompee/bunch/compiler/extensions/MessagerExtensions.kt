package com.tompee.bunch.compiler.extensions

import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic

internal fun Messager.error(element: Element, message: String) {
    printMessage(Diagnostic.Kind.ERROR, message, element)
}