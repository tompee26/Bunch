package com.tompee.bunch

import com.tompee.bunch.annotation.Bunch

@Bunch("Vegetables")
abstract class VegetableInfo {

    @Bunch.Item
    abstract fun pickles(): Int

    @Bunch.Item(name = "tomatoes", tag="ripe_tomatoes", setters=["withABagOf"], getters=["squeeze"])
    abstract fun tomatoes(): Int

    @Bunch.Item(getters = ["cut"])
    abstract fun cabbage(): String

}