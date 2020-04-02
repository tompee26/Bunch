package com.tompee.bunch.compiler

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.ImmutableList
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.Processor
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(BunchProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
@KotlinPoetMetadataPreview
internal class BunchProcessor : BasicAnnotationProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun initSteps(): MutableIterable<ProcessingStep> {
        return ImmutableList.of(
            GeneratorStep(
                processingEnv.elementUtils,
                processingEnv.typeUtils,
                processingEnv.messager,
                processingEnv.filer
            )
        )
    }

//    override fun process(set: MutableSet<out TypeElement>?, env: RoundEnvironment?): Boolean {
//        appComponent = DaggerAppComponent.factory().create(processingEnv)
//        appComponent.inject(this)
//
//        env?.getElementsAnnotatedWith(Bunch::class.java)?.forEach {
//            try {
//                generatorFactory.create(it).generate()
//            } catch (e: ProcessorException) {
//                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, e.message, e.element)
//            }
//        }
//        return true
//    }
}