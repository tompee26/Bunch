package com.tompee.bunch.compiler.di

import dagger.Module
import dagger.Provides
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Singleton
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@Module
internal object ProcessingModule {

    @Singleton
    @Provides
    @JvmStatic
    fun provideMessager(environment: ProcessingEnvironment): Messager {
        return environment.messager
    }

    @Singleton
    @Provides
    @JvmStatic
    fun provideElements(environment: ProcessingEnvironment): Elements {
        return environment.elementUtils
    }

    @Singleton
    @Provides
    @JvmStatic
    fun provideTypes(environment: ProcessingEnvironment): Types {
        return environment.typeUtils
    }
}