package com.tompee.bunch.compiler.di

import com.tompee.bunch.compiler.BunchProcessor
import dagger.BindsInstance
import dagger.Component
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Singleton

@Singleton
@Component(modules = [ProcessingModule::class])
internal interface AppComponent {

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance
            environment: ProcessingEnvironment
        ): AppComponent
    }

    fun inject(processor: BunchProcessor)
}