package com.tompee.bunch.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal val BUNDLE = ClassName("android.os", "Bundle")
internal val BINDER = ClassName("android.os", "IBinder")
internal val CHAR_SEQUENCE_ARRAY = ClassName("kotlin", "Array").parameterizedBy(CHAR_SEQUENCE)
internal val STRING_ARRAY = ClassName("kotlin", "Array").parameterizedBy(STRING)
internal val PARCELABLE = ClassName("android.os", "Parcelable")
internal val PARCELABLE_ARRAY = ClassName("kotlin", "Array").parameterizedBy(PARCELABLE)
internal val SERIALIZABLE = ClassName("java.io", "Serializable")

internal val typeMap = mapOf(
//    BINDER to "Binder",
    BOOLEAN to "Boolean",
    BOOLEAN_ARRAY to "BooleanArray",
    BUNDLE to "Bundle",
    BYTE to "Byte",
    BYTE_ARRAY to "ByteArray",
    CHAR to "Char",
    CHAR_ARRAY to "CharArray",
    CHAR_SEQUENCE to "CharSequence",
    CHAR_SEQUENCE_ARRAY to "CharSequenceArray",
    DOUBLE to "Double",
    DOUBLE_ARRAY to "DoubleArray",
    FLOAT to "Float",
    FLOAT_ARRAY to "FloatArray",
    INT to "Int",
    INT_ARRAY to "IntArray",
    LONG to "Long",
    LONG_ARRAY to "LongArray",
    PARCELABLE to "Parcelable",
    PARCELABLE_ARRAY to "ParcelableArray",
    SERIALIZABLE to "Serializable",
    SHORT to "Short",
    SHORT_ARRAY to "ShortArray",
    STRING to "String",
    STRING_ARRAY to "StringArray"
    // TODO: Size
    // TODO: SizeF
)

internal val primitiveSet = setOf(
    BOOLEAN,
    BOOLEAN_ARRAY,
    BUNDLE,
    BYTE,
    BYTE_ARRAY,
    CHAR,
    CHAR_ARRAY,
    CHAR_SEQUENCE,
    CHAR_SEQUENCE_ARRAY,
    DOUBLE,
    DOUBLE_ARRAY,
    FLOAT,
    FLOAT_ARRAY,
    INT,
    INT_ARRAY,
    LONG,
    LONG_ARRAY,
    SHORT,
    SHORT_ARRAY,
    STRING,
    STRING_ARRAY
)