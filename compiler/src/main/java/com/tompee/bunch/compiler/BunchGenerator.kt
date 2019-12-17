package com.tompee.bunch.compiler

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

internal val BUNDLE = ClassName("android.os", "Bundle")

@KotlinPoetMetadataPreview
internal class BunchGenerator @AssistedInject constructor(
    private val env: ProcessingEnvironment,
    @Assisted private val element: Element
) {
    /**
     * Type element property
     */
    private val property = TypeElementProperties(env, element as TypeElement)

    private val entryFunctionGenerator = EntryFunctionGenerator()

    @AssistedInject.Factory
    interface Factory {
        fun create(element: Element): BunchGenerator
    }

    fun generate() {
        val name = property.getBunchAnnotation().name

        val classSpec = TypeSpec.classBuilder(name)
            .apply { if (property.isInternal()) addModifiers(KModifier.INTERNAL) }
            .addType(entryFunctionGenerator.generate(property))
            .build()

        val fileSpec = FileSpec.builder(property.getPackageName(), name)
            .addType(classSpec)
            .build()
        val kaptKotlinGeneratedDir =
            env.options[BunchProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        fileSpec.writeTo(File(kaptKotlinGeneratedDir, "${name}_Bunch.kt"))
    }
}