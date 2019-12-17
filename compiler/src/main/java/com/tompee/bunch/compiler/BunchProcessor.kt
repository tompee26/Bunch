package com.tompee.bunch.compiler

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.di.AppComponent
import com.tompee.bunch.compiler.di.DaggerAppComponent
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.*
import javax.inject.Inject
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(BunchProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
@KotlinPoetMetadataPreview
internal class BunchProcessor : AbstractProcessor() {

    private lateinit var appComponent: AppComponent

    @Inject
    lateinit var generatorFactory: BunchGenerator.Factory

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Bunch::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(set: MutableSet<out TypeElement>?, env: RoundEnvironment?): Boolean {
        appComponent = DaggerAppComponent.factory().create(processingEnv)
        appComponent.inject(this)

        env?.getElementsAnnotatedWith(Bunch::class.java)?.forEach {
            try {
                generatorFactory.create(it).generate()
            } catch (e: Throwable) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, e.message, it)
            }
        }
        return true
    }
}