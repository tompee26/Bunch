package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.extensions.capitalize
import com.tompee.bunch.compiler.properties.ElementMethod
import java.util.*
import javax.lang.model.element.TypeElement

/**
 * Assertion class generator
 */
@KotlinPoetMetadataPreview
internal class AssertGenerator {

    /**
     * Generates the assert type
     */
    fun generate(element: TypeElement, bunch: Bunch, packageName: String): TypeSpec {
        val listName = LinkedList::class.java.asClassName().parameterizedBy(STRING)

        val typeName = ClassName(ClassName(packageName, bunch.name).toString(), "Assert")
        return TypeSpec.classBuilder("Assert")
            .addModifiers(KModifier.INNER)
            .addProperty(
                PropertySpec.builder("list", listName)
                    .initializer("${LinkedList::class.java.simpleName}<${String::class.java.simpleName}>()")
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
            .addFunctions(generateAsserts(typeName, element))
            .build()
    }

    /**
     * Generates the assertion methods.
     *
     * @return the list of assertion methods
     */
    private fun generateAsserts(typeName: TypeName, element: TypeElement): List<FunSpec> {
        return ElementMethod(element).getAllInformation()
            .map {
                val paramName = if (it.item.name.isEmpty()) it.kotlin.name else it.item.name
                val tag = if (it.item.tag.isEmpty()) "tag_${it.kotlin.name}" else it.item.tag

                FunSpec.builder("has${paramName.capitalize()}")
                    .returns(typeName)
                    .addStatement("return Assert().add(\"$tag\")")
                    .build()
            }
    }
}