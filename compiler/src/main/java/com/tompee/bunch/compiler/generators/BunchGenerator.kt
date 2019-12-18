package com.tompee.bunch.compiler.generators

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.BunchProcessor
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
internal class BunchGenerator @AssistedInject constructor(
    private val env: ProcessingEnvironment,
    @Assisted private val element: Element
) {
    private val javaProperties = JavaProperties(env, element as TypeElement)
    private val kotlinProperties = KotlinProperties(env, element as TypeElement)

    private val entryFunctionGenerator = CompanionGenerator()

    @AssistedInject.Factory
    interface Factory {
        fun create(element: Element): BunchGenerator
    }

    fun generate() {
        val name = javaProperties.getBunchAnnotation().name
        val classSpec = TypeSpec.classBuilder(name)
            .apply { if (kotlinProperties.isInternal()) addModifiers(KModifier.INTERNAL) }
            .addType(entryFunctionGenerator.generate(javaProperties, kotlinProperties))
            .build()

        val fileSpec = FileSpec.builder(javaProperties.getPackageName(), name)
            .addType(classSpec)
            .build()
        val kaptKotlinGeneratedDir =
            env.options[BunchProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        fileSpec.writeTo(File(kaptKotlinGeneratedDir, "${name}_Bunch.kt"))
    }
}