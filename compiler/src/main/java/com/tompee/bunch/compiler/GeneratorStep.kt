package com.tompee.bunch.compiler

import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.generators.AssertGenerator
import com.tompee.bunch.compiler.generators.CompanionGenerator
import com.tompee.bunch.compiler.generators.MethodGenerator
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
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

    private val classInspector = ElementsClassInspector.create(elements, types)

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
        val javaProperties = JavaProperties(element, elements)
        val kotlinProperties = KotlinProperties(element, elements, classInspector)

        val name = javaProperties.getBunchAnnotation().name

        val fileSpec = FileSpec.builder(javaProperties.getPackageName(), name)
            .addType(generateClassSpec(name, kotlinProperties, javaProperties))
            .build()
        fileSpec.writeTo(filer)
    }

    private fun generateClassSpec(
        name: String,
        kotlinProperties: KotlinProperties,
        javaProperties: JavaProperties
    ): TypeSpec {
        val constructor = FunSpec.constructorBuilder()
            .addParameter("bundle", BUNDLE)
            .build()

        val methodGenerator = MethodGenerator()
        return TypeSpec.classBuilder(name)
            .apply { if (kotlinProperties.isInternal()) addModifiers(KModifier.INTERNAL) }
            .primaryConstructor(constructor)
            .addProperty(
                PropertySpec.builder("bundle", BUNDLE)
                    .initializer("bundle")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addType(AssertGenerator().generate(javaProperties, kotlinProperties))
            .addType(CompanionGenerator().generate(javaProperties, kotlinProperties))
            .addFunctions(methodGenerator.generateAsserts(javaProperties, kotlinProperties))
            .addFunctions(methodGenerator.generateSetters(javaProperties, kotlinProperties))
            .addFunctions(methodGenerator.generateGetters(javaProperties, kotlinProperties))
            .addFunction(methodGenerator.generateCollector())
            .build()
    }
}