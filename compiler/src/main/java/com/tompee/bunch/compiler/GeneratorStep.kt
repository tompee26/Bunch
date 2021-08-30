package com.tompee.bunch.compiler

import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.extensions.getPackageName
import com.tompee.bunch.compiler.extensions.isInternal
import com.tompee.bunch.compiler.extensions.parseAnnotation
import com.tompee.bunch.compiler.generators.AssertGenerator
import com.tompee.bunch.compiler.generators.CompanionGenerator
import com.tompee.bunch.compiler.generators.MethodGenerator
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@KotlinPoetMetadataPreview
internal class GeneratorStep(
    private val elements: Elements,
    private val types: Types,
    private val messager: Messager,
    private val filer: Filer
) : BasicAnnotationProcessor.ProcessingStep {

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<out Element> {
        elementsByAnnotation.entries()
            .map { it.value }
            .forEach {
                try {
                    generate(it as TypeElement)
                } catch (e: ProcessorException) {
                    messager.printMessage(Diagnostic.Kind.ERROR, e.message, e.element)
                }
            }
        return mutableSetOf()
    }

    override fun annotations(): MutableSet<out Class<out Annotation>> {
        return mutableSetOf(Bunch::class.java)
    }

    private fun generate(element: TypeElement) {
        val bunch = element.parseAnnotation<Bunch>()!!
        val packageName = element.getPackageName(elements)

        val fileSpec = FileSpec.builder(packageName, bunch.name)
            .addType(generateClassSpec(bunch, packageName, element))
            .build()
        fileSpec.writeTo(filer)
    }

    private fun generateClassSpec(
        bunch: Bunch,
        packageName: String,
        element: TypeElement
    ): TypeSpec {
        val constructor = FunSpec.constructorBuilder()
            .addParameter("bundle", BUNDLE)
            .addModifiers(KModifier.PRIVATE)
            .build()

        val methodGenerator = MethodGenerator(element, types, elements, bunch, packageName)
        return TypeSpec.classBuilder(bunch.name)
            .addOriginatingElement(element)
            .apply { if (element.isInternal) addModifiers(KModifier.INTERNAL) }
            .primaryConstructor(constructor)
            .addProperty(
                PropertySpec.builder("bundle", BUNDLE)
                    .initializer("bundle")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addType(AssertGenerator().generate(element, bunch, packageName))
            .addType(CompanionGenerator().generate(element, bunch, packageName))
            .addFunctions(methodGenerator.generateAsserts())
            .addFunctions(methodGenerator.generateSetters())
            .addFunctions(methodGenerator.generateGetters())
            .addFunction(methodGenerator.generateCollector())
            .build()
    }
}