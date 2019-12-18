package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.*
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties

@KotlinPoetMetadataPreview
internal class CompanionGenerator {

    fun generate(jProp: JavaProperties, kProp: KotlinProperties): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addFunctions(generateEntryFunction(jProp, kProp))
            .addFunction(createDuplicateFunction())
            .addFunction(crossFunction(jProp))
            .addFunctions(createSetters())
            .addFunction(createParcelableSetter())
            .addFunction(createParcelablArrayeSetter())
            .addFunction(createSerializableSetter())
//            .addFunctions(createGetters())
            .build()
    }

    private fun generateEntryFunction(
        jProp: JavaProperties,
        kProp: KotlinProperties
    ): List<FunSpec> {
        return kProp.getTypeSpec().funSpecs
            .asSequence()
            .map { funSpec ->
                val jFun = jProp.getMethods().first { it.simpleName.toString() == funSpec.name }
                funSpec to jFun
            }
            .mapNotNull {
                val annotation = JavaProperties.getItemAnnotation(it.second)
                if (annotation == null) null else it.first to annotation
            }
            .flatMap { pair ->
                val (funSpec, annotation) = pair
                val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
                val prefixes =
                    if (annotation.setters.isEmpty()) arrayOf("with") else annotation.setters
                prefixes.asSequence().map {
                    val functionName = "$it${paramName.capitalize()}"
                    FunSpec.builder(functionName)
                        .addParameter(
                            ParameterSpec(
                                paramName,
                                funSpec.returnType ?: throw Throwable("Input type not supported")
                            )
                        )
                        .returns(jProp.getTargetTypeName())
                        .addStatement("return ${jProp.getTargetTypeName()}(Bundle()).$functionName($paramName)".wrapProof())
                        .build()
                }
            }.toList()
    }

    private fun createDuplicateFunction(): FunSpec {
        return FunSpec.builder("duplicate")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .returns(BUNDLE)
            .addStatement("return if (this == Bundle.EMPTY) Bundle() else clone() as Bundle".wrapProof())
            .build()
    }

    private fun crossFunction(jProp: JavaProperties): FunSpec {
        return FunSpec.builder("from")
            .addParameter("bundle", BUNDLE)
            .returns(jProp.getTargetTypeName())
            .addStatement("return ${jProp.getTargetTypeName()}(bundle.duplicate())".wrapProof())
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

    private fun createParcelableSetter(): FunSpec {
        return FunSpec.builder("insertParcelable")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .addParameter("tag", STRING)
            .addParameter("value", PARCELABLE)
            .addStatement("putParcelable(tag, value)".wrapProof())
            .build()
    }

    private fun createParcelablArrayeSetter(): FunSpec {
        return FunSpec.builder("insertParcelableArray")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .addParameter("tag", STRING)
            .addParameter("value", PARCELABLE_ARRAY)
            .addStatement("putParcelableArray(tag, value)".wrapProof())
            .build()
    }

    private fun createSerializableSetter(): FunSpec {
        return FunSpec.builder("insertSerializable")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .addParameter("tag", STRING)
            .addParameter("value", SERIALIZABLE)
            .addStatement("putSerializable(tag, value)".wrapProof())
            .build()
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