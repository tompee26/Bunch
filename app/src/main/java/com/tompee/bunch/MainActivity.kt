package com.tompee.bunch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Vegetables.withPickles(2)
            .withABagOfTomatoes(2)
            .collect()
        val bundle = Bundle.EMPTY
        val tomatoes = Vegetables.from(bundle).squeezeTomatoes()

        val cabbage = Vegetables.from(bundle).cutCabbageOrThrow()
    }
}
