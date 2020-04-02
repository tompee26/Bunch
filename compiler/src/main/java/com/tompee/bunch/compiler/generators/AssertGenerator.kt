package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.ProcessorException
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind

/**
 * Assertion class generator
 */
@KotlinPoetMetadataPreview
internal class AssertGenerator {

    /**
     * Generates the assert type
     */
    fun generate(jProp: JavaProperties, kProp: KotlinProperties): TypeSpec {
        val listName = ClassName("java.util", "LinkedList")
            .parameterizedBy(STRING)

        val typeName = ClassName("${jProp.getTargetTypeName()}", "Assert")
        return TypeSpec.classBuilder("Assert")
            .addModifiers(KModifier.INNER)
            .addProperty(
                PropertySpec.builder("list", listName)
                    .initializer("$listName()")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addFunction(
                FunSpec.builder("add")
                    .returns(typeName)
                    .addParameter("tag", STRING)
                    .addStatement("return apply { list.add(tag) }")
                    .build()
            )
            .addFunction(
                FunSpec.builder("assert")
                    .addStatement("val keys = bundle.keySet()")
                    .beginControlFlow("list.forEach")
                    .addStatement("if (it !in keys) throw IllegalStateException(\"\$it not found\")")
                    .endControlFlow()
                    .build()
            )
            .addFunctions(generateAsserts(jProp, kProp))
            .build()
    }

    /**
     * Generates the assertion methods.
     *
     * @param jProp Java properties
     * @param kProp Kotlin properties
     * @return the list of assertion methods
     */
    private fun generateAsserts(jProp: JavaProperties, kProp: KotlinProperties): List<FunSpec> {
        val typeName = ClassName("${jProp.getTargetTypeName()}", "Assert")

        val companionSpecs =
            kProp.getTypeSpec().typeSpecs.firstOrNull { it.isCompanion }?.funSpecs ?: emptyList()
        return kProp.getTypeSpec().funSpecs.plus(companionSpecs)
            .asSequence()
            .map { pairWithJavaMethod(it, jProp) }
            .filter { JavaProperties.getItemAnnotation(it.second) != null }
            .map {
                val (funSpec, element) = it
                val annotation = JavaProperties.getItemAnnotation(element)!!
                val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
                val tag = if (annotation.tag.isEmpty()) "tag_${funSpec.name}" else annotation.tag

                FunSpec.builder("has${paramName.capitalize()}")
                    .returns(typeName)
                    .addStatement("return Assert().add(\"$tag\")")
                    .build()
            }
            .toList()
    }

    /**
     * Pairs a kotlin method in the class and/or the companion object with its Java symbol counterpart.
     * Both are necessary because kotlin types are easier to work with in terms of type names and
     * Java symbols are necessary for type hierarchy checking.
     *
     * @param funSpec kotlin function spec
     * @param jProp Java property
     * @return the pair of kotlin and java method
     */
    private fun pairWithJavaMethod(
        funSpec: FunSpec,
        jProp: JavaProperties
    ): Pair<FunSpec, Element> {
        val enclosedElements =
            jProp.getElement().enclosedElements.filter { it.kind == ElementKind.CLASS }
                .flatMap { classes -> classes.enclosedElements.filter { it.kind == ElementKind.METHOD } }
        val jFun =
            jProp.getMethods().plus(enclosedElements)
                .firstOrNull { it.simpleName.toString() == funSpec.name }
                ?: throw ProcessorException(
                    jProp.getElement(),
                    "Some functions cannot be interpreted"
                )
        return funSpec to jFun
    }
}