package com.tompee.bunch.compiler

import javax.lang.model.element.Element

class ProcessorException(val element: Element, message: String) : Throwable(message)