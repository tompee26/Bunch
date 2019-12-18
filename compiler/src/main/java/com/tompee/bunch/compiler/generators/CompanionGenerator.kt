package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.BUNDLE
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
import com.tompee.bunch.compiler.typeMap

@KotlinPoetMetadataPreview
internal class CompanionGenerator {

    fun generate(jProp: JavaProperties, kProp: KotlinProperties): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addFunctions(generateEntryFunction(jProp, kProp))
            .addFunction(createDuplicateFunction())
            .addFunctions(createSetters())
//            .addFunctions(createGetters())
            .build()
    }

    private fun generateEntryFunction(
        jProp: JavaProperties,
        kProp: KotlinProperties
    ): List<FunSpec> {
        return kProp.getTypeSpec().funSpecs.zip(jProp.getMethods()).toMap()
            .mapNotNull {
                val annotation = JavaProperties.getItemAnnotation(it.value)
                if (annotation == null) null else it.key to annotation
            }
            .flatMap { pair ->
                val (funSpec, annotation) = pair
                val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
                val prefixes =
                    if (annotation.setters.isEmpty()) arrayOf("with") else annotation.setters
                prefixes.map {
                    val functionName = "$it${paramName.capitalize()}"
                    FunSpec.builder(functionName)
                        .addParameter(ParameterSpec(paramName, funSpec.returnType!!))
                        .returns(jProp.getTargetTypeName())
                        .addStatement("return ${jProp.getTargetTypeName()}(Bundle()).$functionName($paramName)".wrapProof())
                        .build()
                }
            }
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