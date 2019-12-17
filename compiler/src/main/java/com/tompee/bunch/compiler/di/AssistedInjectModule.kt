package com.tompee.bunch.compiler.di

import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Module

@AssistedModule
@Module(includes = [AssistedInject_AssistedInjectModule::class])
internal interface AssistedInjectModule