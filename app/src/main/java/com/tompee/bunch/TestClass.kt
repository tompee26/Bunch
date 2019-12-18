package com.tompee.bunch

import com.tompee.bunch.annotation.Bunch

@Bunch("Args")
internal abstract class TestClass {

    @Bunch.Item(name = "myIndex", setters = ["put", "add", "seasonWith"])
    abstract fun index(): Int

    abstract fun stop()
}