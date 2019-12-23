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
    private val methodGenerator: MethodGenerator,
    private val companionGenerator: CompanionGenerator,
    private val javaPropFactory: JavaProperties.Factory,
    private val kotlinPropFactory: KotlinProperties.Factory,
    @Assisted private val element: Element
) {
    private val javaProperties by lazy { javaPropFactory.create(element as TypeElement) }
    private val kotlinProperties by lazy { kotlinPropFactory.create(element as TypeElement) }

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
            .addType(companionGenerator.generate(javaProperties, kotlinProperties))
            .addFunctions(methodGenerator.generateSetters(javaProperties, kotlinProperties))
            .addFunctions(methodGenerator.generateGetters(javaProperties, kotlinProperties))
            .addFunction(methodGenerator.generateCollector())
            .build()

        val fileSpec = FileSpec.builder(javaProperties.getPackageName(), name)
            .addType(classSpec)
            .build()
        val kaptKotlinGeneratedDir =
            env.options[BunchProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        fileSpec.writeTo(File(kaptKotlinGeneratedDir, "${name}_Bunch.kt"))
    }
}