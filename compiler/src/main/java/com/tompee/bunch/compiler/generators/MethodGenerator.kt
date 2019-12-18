package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.primitiveSet
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
import javax.inject.Inject

@KotlinPoetMetadataPreview
internal class MethodGenerator @Inject constructor(private val classInspector: ClassInspector) {

    fun generateSetters(jProp: JavaProperties, kProp: KotlinProperties): List<FunSpec> {
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
                val tag = if (annotation.tag.isEmpty()) "tag_${funSpec.name}" else annotation.tag
                prefixes.asSequence().map {
                    val functionName = "$it${paramName.capitalize()}"
                    val statement = generatePrimitiveStatement(tag, funSpec, paramName)
                        ?: generateSpecialStatement(tag, funSpec, paramName)
                        ?: throw Throwable("Input type is not supported")
                    FunSpec.builder(functionName)
                        .addParameter(
                            ParameterSpec(
                                paramName,
                                funSpec.returnType ?: throw Throwable("Input type not supported")
                            )
                        )
                        .returns(jProp.getTargetTypeName())
                        .addStatement(statement)
                        .build()
                }
            }.toList()
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

    private fun generateSpecialStatement(
        tag: String,
        funSpec: FunSpec,
        paramName: String
    ): String? {
        return null
    }
}