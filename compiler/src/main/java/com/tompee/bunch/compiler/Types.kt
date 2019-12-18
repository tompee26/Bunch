package com.tompee.bunch.compiler

import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING

internal val BUNDLE = ClassName("android.os", "Bundle")
internal val BINDER = ClassName("android.os", "IBinder")
internal val CHAR_SEQUENCE_ARRAY = ClassName("kotlin", "Array").parameterizedBy(CHAR_SEQUENCE)
internal val STRING_ARRAY = ClassName("kotlin", "Array").parameterizedBy(STRING)

