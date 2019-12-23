package com.tompee.bunch.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal val BUNDLE = ClassName("android.os", "Bundle")
internal val BINDER = ClassName("android.os", "IBinder")
internal val CHAR_SEQUENCE_ARRAY = ClassName("kotlin", "Array").parameterizedBy(CHAR_SEQUENCE)
internal val STRING_ARRAY = ClassName("kotlin", "Array").parameterizedBy(STRING)
internal val PARCELABLE = ClassName("android.os", "Parcelable")
internal val PARCELABLE_LIST = ClassName("kotlin.collections", "List").parameterizedBy(PARCELABLE)
internal val SERIALIZABLE = ClassName("java.io", "Serializable")
internal val JAVA_LIST = ClassName("java.util", "List")
internal val JAVA_ENUM = ClassName("java.lang", "Enum")

internal val defaultValueMap = mapOf(
    SHORT to "Short",
    LONG to "Long",
    INT to "Int",
    FLOAT to "Float",
    DOUBLE to "Double",
    CHAR to "Char",
    BYTE to "Byte",
    BOOLEAN to "Boolean"
)

internal val nullableDefaultValueMap = mapOf(
    STRING to "String"
)

internal val nonDefaultValueMap = mapOf(
    STRING_ARRAY to "StringArray",
    SHORT_ARRAY to "ShortArray",
    LONG_ARRAY to "LongArray",
    INT_ARRAY to "IntArray",
    FLOAT_ARRAY to "FloatArray",
    DOUBLE_ARRAY to "DoubleArray",
    CHAR_SEQUENCE_ARRAY to "CharSequenceArray",
    CHAR_SEQUENCE to "CharSequence",
    CHAR_ARRAY to "CharArray",
    BYTE_ARRAY to "ByteArray",
    BUNDLE to "Bundle",
    BOOLEAN_ARRAY to "BooleanArray"
)

internal val typeMap = mutableMapOf<TypeName, String>(
    // TODO: Binder
    PARCELABLE to "Parcelable",
    SERIALIZABLE to "Serializable"
    // TODO: Size
    // TODO: SizeF
).apply {
    putAll(defaultValueMap)
    putAll(nullableDefaultValueMap)
    putAll(nonDefaultValueMap)
}
