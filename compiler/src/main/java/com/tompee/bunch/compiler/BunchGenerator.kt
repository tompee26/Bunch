package com.tompee.bunch.compiler

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

internal class BunchGenerator @AssistedInject constructor(
    private val env: ProcessingEnvironment,
    @Assisted private val element: Element
) {

    @AssistedInject.Factory
    interface Factory {
        fun create(element: Element): BunchGenerator
    }
}