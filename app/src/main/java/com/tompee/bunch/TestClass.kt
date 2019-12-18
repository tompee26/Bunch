package com.tompee.bunch

import android.os.Bundle
import com.tompee.bunch.annotation.Bunch

@Bunch("Args")
internal abstract class TestClass {

//    @Bunch.Item(name = "myIndex", setters = ["put", "add", "seasonWith"])
//    abstract fun index(): Int

    @Bunch.Item
    abstract fun boolean1(): Boolean

    @Bunch.Item
    abstract fun booleanArray(): BooleanArray

    @Bunch.Item
    abstract fun bundle(): Bundle

    @Bunch.Item
    abstract fun byte1(): Byte

    @Bunch.Item
    abstract fun byteArray(): ByteArray

    @Bunch.Item
    abstract fun char1(): Char

    @Bunch.Item
    abstract fun charArray(): CharArray

    @Bunch.Item
    abstract fun charSequence(): CharSequence

    @Bunch.Item
    abstract fun charSequenceArray(): Array<CharSequence>

    @Bunch.Item
    abstract fun double1(): Double

    @Bunch.Item
    abstract fun doubleArray(): DoubleArray

    @Bunch.Item
    abstract fun float1(): Float

    @Bunch.Item
    abstract fun floatArray(): FloatArray

    @Bunch.Item
    abstract fun int1(): Int

    @Bunch.Item
    abstract fun intArray(): IntArray

    @Bunch.Item
    abstract fun long1(): Long

    @Bunch.Item
    abstract fun longArray(): LongArray

    @Bunch.Item
    abstract fun short1(): Short

    @Bunch.Item
    abstract fun shortArray(): ShortArray

    @Bunch.Item
    abstract fun string1(): String

    @Bunch.Item
    abstract fun stringArray(): Array<String>

//    @Bunch.Item
//    abstract fun unit(): Array<String>

    abstract fun stop()
}