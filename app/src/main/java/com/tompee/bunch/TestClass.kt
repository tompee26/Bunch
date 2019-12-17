package com.tompee.bunch

import android.os.Bundle
import com.tompee.bunch.annotation.Bunch

@Bunch("Args")
internal abstract class TestClass {

    abstract fun index() : Bundle
}