package com.tompee.bunch

import android.os.Bundle
import com.tompee.bunch.annotation.Bunch

@Bunch("Args")
internal abstract class TestClass {

//    @Bunch.Item(name = "myIndex", setters = ["put", "add", "seasonWith"])
//    abstract fun index(): Int

    @Bunch.Item
    abstract fun boolean(): Boolean

    @Bunch.Item
    abstract fun booleanArray(): BooleanArray

    @Bunch.Item
    abstract fun bundle(): Bundle

    @Bunch.Item
    abstract fun byte(): Byte

    @Bunch.Item
    abstract fun byteArray(): ByteArray

    @Bunch.Item
    abstract fun char(): Char

    @Bunch.Item
    abstract fun charArray(): CharArray

    @Bunch.Item
    abstract fun charSequence(): CharSequence

    @Bunch.Item
    abstract fun charSequenceArray(): Array<CharSequence>

    @Bunch.Item
    abstract fun double(): Double

    @Bunch.Item
    abstract fun doubleArray(): DoubleArray

    @Bunch.Item
    abstract fun float(): Float

    @Bunch.Item
    abstract fun floatArray(): FloatArray

    @Bunch.Item
    abstract fun int(): Int

    @Bunch.Item
    abstract fun intArray(): IntArray

    @Bunch.Item
    abstract fun long(): Long

    @Bunch.Item
    abstract fun longArray(): LongArray

    @Bunch.Item
    abstract fun short(): Short

    @Bunch.Item
    abstract fun shortArray(): ShortArray

    @Bunch.Item
    abstract fun string(): String

    @Bunch.Item
    abstract fun stringArray(): Array<String>

//    @Bunch.Item
//    abstract fun unit(): Array<String>

    abstract fun stop()
}