package com.tompee.bunch.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.extensions.wrapProof

@KotlinPoetMetadataPreview
internal class CompanionGenerator {

    companion object {

        private val typeMap = mapOf(
            BINDER to "Binder",
            BOOLEAN to "Boolean",
            BOOLEAN_ARRAY to "BooleanArray",
            BUNDLE to "Bundle",
            BYTE to "Byte",
            BYTE_ARRAY to "ByteArray",
            CHAR to "Char",
            CHAR_ARRAY to "CharArray",
            CHAR_SEQUENCE to "CharSequence",
            CHAR_SEQUENCE_ARRAY to "CharSequenceArray",
            DOUBLE to "Double",
            DOUBLE_ARRAY to "DoubleArray",
            FLOAT to "Float",
            FLOAT_ARRAY to "FloatArray",
            INT to "Int",
            INT_ARRAY to "IntArray",
            LONG to "Long",
            LONG_ARRAY to "LongArray",
            // TODO: Parcelable
            // TODO: Parcelable array
            // TODO: Serializable
            SHORT to "Short",
            SHORT_ARRAY to "ShortArray",
            STRING to "String",
            STRING_ARRAY to "StringArray"
            // TODO: Size
            // TODO: SizeF
        )
    }

    fun generate(element: TypeElementProperties): TypeSpec {
        val funSpecs = element.getTypeSpec().funSpecs
            .filter { funSpecs ->
                funSpecs.annotations.isNotEmpty() &&
                        funSpecs.annotations.any { it.className == Bunch.Item::class.asClassName() }
            }
            .map { generateEntryFunction(it) }
        return TypeSpec.companionObjectBuilder()
            .addFunctions(funSpecs)
            .addFunction(createDuplicateFunction())
            .addFunctions(createSetters())
            .addFunctions(createGetters())
            .build()
    }

    private fun generateEntryFunction(funSpec: FunSpec): FunSpec {
        return FunSpec.builder("with${funSpec.name.capitalize()}")
            .addParameter(ParameterSpec(funSpec.name, funSpec.returnType!!))
            .returns(BUNDLE)
            .build()
    }

    private fun createDuplicateFunction(): FunSpec {
        return FunSpec.builder("duplicate")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .returns(BUNDLE)
            .addStatement("return if (this == Bundle.EMPTY) Bundle() else clone() as Bundle".wrapProof())
            .build()
    }

    private fun createSetters(): List<FunSpec> {
        return typeMap.map {
            FunSpec.builder("insert")
                .addModifiers(KModifier.PRIVATE)
                .receiver(BUNDLE)
                .addParameter("tag", STRING)
                .addParameter("value", it.key)
                .addStatement("put${it.value}(tag, value)".wrapProof())
                .build()
        }
    }

    private fun createGetters(): List<FunSpec> {
        return typeMap.map {
            FunSpec.builder("get${it.value}")
                .addModifiers(KModifier.PRIVATE)
                .receiver(BUNDLE)
                .addParameter("tag", STRING)
                .returns(it.key.copy(true))
                .addStatement("return get${it.value}(tag)".wrapProof())
                .build()
        }
    }
}