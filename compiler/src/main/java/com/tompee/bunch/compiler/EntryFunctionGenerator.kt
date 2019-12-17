package com.tompee.bunch.compiler

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.annotation.Bunch

@KotlinPoetMetadataPreview
internal class EntryFunctionGenerator {

    fun generate(element: TypeElementProperties): TypeSpec {
        val funSpecs = element.getTypeSpec().funSpecs
            .filter { funSpecs ->
                funSpecs.annotations.isNotEmpty() &&
                        funSpecs.annotations.any { it.className == Bunch.Item::class.asClassName() }
            }
            .map { generateEntryFunction(it) }
        return TypeSpec.companionObjectBuilder()
            .addFunctions(funSpecs)
            .build()
    }

    private fun generateEntryFunction(funSpec: FunSpec): FunSpec {
        return FunSpec.builder("with${funSpec.name.capitalize()}")
            .addParameter(ParameterSpec(funSpec.name, funSpec.returnType!!))
            .returns(BUNDLE)
            .build()
    }
}