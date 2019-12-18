package com.tompee.bunch.compiler.generators

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.BUNDLE
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
    private val methodGenerator = MethodGenerator()

    @AssistedInject.Factory
    interface Factory {
        fun create(element: Element): BunchGenerator
    }

    fun generate() {
        val name = javaProperties.getBunchAnnotation().name
        val constructor = FunSpec.constructorBuilder()
            .addParameter("bundle", BUNDLE)
            .build()

        val classSpec = TypeSpec.classBuilder(name)
            .apply { if (kotlinProperties.isInternal()) addModifiers(KModifier.INTERNAL) }
            .primaryConstructor(constructor)
            .addProperty(
                PropertySpec.builder("bundle", BUNDLE)
                    .initializer("bundle")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addType(entryFunctionGenerator.generate(javaProperties, kotlinProperties))
            .addFunctions(methodGenerator.generateSetters(javaProperties, kotlinProperties))
            .build()

        val fileSpec = FileSpec.builder(javaProperties.getPackageName(), name)
            .addType(classSpec)
            .build()
        val kaptKotlinGeneratedDir =
            env.options[BunchProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        fileSpec.writeTo(File(kaptKotlinGeneratedDir, "${name}_Bunch.kt"))
    }
}