package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.extensions.capitalize
import com.tompee.bunch.compiler.properties.TypeElementProperty

/**
 * Assertion class generator
 */
@KotlinPoetMetadataPreview
internal class AssertGenerator {

    /**
     * Generates the assert type
     */
    fun generate(prop: TypeElementProperty): TypeSpec {
        val listName = ClassName("java.util", "LinkedList")
            .parameterizedBy(STRING)

        val typeName = ClassName("${prop.targetTypeName}", "Assert")
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
            .addFunctions(generateAsserts(prop))
            .build()
    }

    /**
     * Generates the assertion methods.
     *
     * @return the list of assertion methods
     */
    private fun generateAsserts(prop: TypeElementProperty): List<FunSpec> {
        val typeName = ClassName("${prop.targetTypeName}", "Assert")

        return prop.getFunSpecElementPairList()
            .map { (funSpec, element) ->
                val annotation = TypeElementProperty.getItemAnnotation(element)!!
                val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
                val tag = if (annotation.tag.isEmpty()) "tag_${funSpec.name}" else annotation.tag

                FunSpec.builder("has${paramName.capitalize()}")
                    .returns(typeName)
                    .addStatement("return Assert().add(\"$tag\")")
                    .build()
            }
            .toList()
    }


}