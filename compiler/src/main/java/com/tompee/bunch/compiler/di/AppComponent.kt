package com.tompee.bunch.compiler.di

import dagger.BindsInstance
import dagger.Component
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Singleton

@Singleton
@Component
internal interface AppComponent {

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance
            environment: ProcessingEnvironment
        ): AppComponent
    }
}