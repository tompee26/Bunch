package com.tompee.bunch

import com.tompee.bunch.annotation.Bunch

@Bunch("Args")
internal abstract class TestClass {

    @Bunch.Item(name = "myIndex")
    abstract fun index(): Int
}