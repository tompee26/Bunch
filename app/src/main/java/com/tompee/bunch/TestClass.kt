package com.tompee.bunch

import android.os.Bundle
import android.os.Parcelable
import com.tompee.bunch.annotation.Bunch
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

open class ParentParcelable(val value: Int)

@Parcelize
class ChildParcelable : ParentParcelable(0), Parcelable

class MySerializable : ParentParcelable(0), Serializable

enum class MyEnum {
    ITEM_1,
    ITEM_2
}

@Bunch("Args")
abstract class TestClass {

    companion object {

        @Bunch.Item
        fun booleanWithDefault(): Boolean = false

        @Bunch.Item
        fun booleanArrayWithDefault(): BooleanArray = booleanArrayOf()

        @Bunch.Item
        fun bundleWithDefault(): Bundle = Bundle.EMPTY

        @Bunch.Item
        fun byteWithDefault(): Byte = 1

        @Bunch.Item
        fun byteArrayWithDefault(): ByteArray = byteArrayOf()

        @Bunch.Item
        fun charWithDefault(): Char = 'a'

        @Bunch.Item
        fun charArrayWithDefault(): CharArray = charArrayOf()

        @Bunch.Item
        fun charSequenceWithDefault(): CharSequence = "abs"

        @Bunch.Item
        fun charSequenceArrayWithDefault(): Array<CharSequence> = arrayOf()

        @Bunch.Item
        fun doubleWithDefault(): Double = 0.0

        @Bunch.Item
        fun doubleArrayWithDefault(): DoubleArray = doubleArrayOf()

        @Bunch.Item
        fun floatWithDefault(): Float = 0.0f

        @Bunch.Item
        fun floatArrayWithDefault(): FloatArray = floatArrayOf()

        @Bunch.Item
        fun indexWithDefault(): Int = -1

        @Bunch.Item
        fun intWithDefault(): Int = 12

        @Bunch.Item
        fun intArrayWithDefault(): IntArray = intArrayOf()

        @Bunch.Item
        fun longWithDefault(): Long = 20L

        @Bunch.Item
        fun longArrayWithDefault(): LongArray = longArrayOf()

        @Bunch.Item
        fun shortWithDefault(): Short = 0

        @Bunch.Item
        fun shortArrayWithDefault(): ShortArray = shortArrayOf()

        @Bunch.Item
        fun stringWithDefault(): String = ""

        @Bunch.Item
        fun stringArrayWithDefault(): Array<String> = arrayOf()

        @Bunch.Item
        fun parcelableListWithDefault(): List<ChildParcelable> = listOf()

        @Bunch.Item
        fun myEnumWithDefault(): MyEnum = MyEnum.ITEM_1

        @Bunch.Item
        fun parcelableWithDefault(): ChildParcelable = ChildParcelable()

        @Bunch.Item
        fun serializableWithDefault(): MySerializable = MySerializable()
    }

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

    @Bunch.Item(name = "myIndex", setters = ["put", "add", "seasonWith"])
    abstract fun index(): Int

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

    @Bunch.Item
    abstract fun parcelableList(): List<ChildParcelable>

    @Bunch.Item
    abstract fun myEnum(): MyEnum

    @Bunch.Item("myChild", tag = "_of_war")
    abstract fun parcelable(): ChildParcelable

    @Bunch.Item
    abstract fun serializable(): MySerializable

//    abstract fun stop()
}