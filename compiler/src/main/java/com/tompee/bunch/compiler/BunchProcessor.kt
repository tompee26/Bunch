package com.tompee.bunch.compiler

import com.google.auto.service.AutoService
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.di.AppComponent
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(BunchProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
internal class BunchProcessor : AbstractProcessor() {

    private lateinit var appComponent: AppComponent

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Bunch::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(set: MutableSet<out TypeElement>?, env: RoundEnvironment?): Boolean {
        return true
    }
}