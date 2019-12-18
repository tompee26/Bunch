package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.primitiveSet
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties

@KotlinPoetMetadataPreview
internal class MethodGenerator {

    fun generateSetters(jProp: JavaProperties, kProp: KotlinProperties): List<FunSpec> {
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
                val tag = if (annotation.tag.isEmpty()) "tag_${funSpec.name}" else annotation.tag
                prefixes.map {
                    val functionName = "$it${paramName.capitalize()}"
                    val statement = generatePrimitiveStatement(tag, funSpec, paramName)
                        ?: throw IllegalStateException("Return type is not supported")
                    FunSpec.builder(functionName)
                        .addParameter(ParameterSpec(paramName, funSpec.returnType!!))
                        .returns(jProp.getTargetTypeName())
                        .addStatement(statement)
                        .build()
                }
            }
    }

    private fun generatePrimitiveStatement(
        tag: String,
        funSpec: FunSpec,
        paramName: String
    ): String? {
        return if (funSpec.returnType in primitiveSet) {
            "return apply { bundle.insert(\"$tag\", $paramName)}".wrapProof()
        } else null
    }
}