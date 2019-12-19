package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.tompee.bunch.compiler.PARCELABLE
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.primitiveSet
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
import javax.inject.Inject
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Types

@KotlinPoetMetadataPreview
internal class MethodGenerator @Inject constructor(
    private val types: Types,
    private val classInspector: ClassInspector
) {

    fun generateSetters(jProp: JavaProperties, kProp: KotlinProperties): List<FunSpec> {
        return kProp.getTypeSpec().funSpecs
            .asSequence()
            .map { funSpec -> pairWithJavaMethod(funSpec, jProp) }
            .filter { JavaProperties.getItemAnnotation(it.second) != null }
            .flatMap { generateSequence(it.first, it.second, jProp.getTargetTypeName()) }
            .toList()
    }

    private fun pairWithJavaMethod(
        funSpec: FunSpec,
        jProp: JavaProperties
    ): Pair<FunSpec, Element> {
        val jFun = jProp.getMethods().firstOrNull { it.simpleName.toString() == funSpec.name }
            ?: throw Throwable("Some functions cannot be interpreted")
        return funSpec to jFun
    }

    private fun generateSequence(
        funSpec: FunSpec,
        element: Element,
        name: TypeName
    ): Sequence<FunSpec> {
        val annotation = JavaProperties.getItemAnnotation(element)!!
        val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
        val prefixes =
            if (annotation.setters.isEmpty()) arrayOf("with") else annotation.setters
        val tag = if (annotation.tag.isEmpty()) "tag_${funSpec.name}" else annotation.tag
        val method = element as Symbol.MethodSymbol
        val type = types.asElement(method.returnType) as TypeElement

        return prefixes.asSequence().map {
            val functionName = "$it${paramName.capitalize()}"
            val statement = generatePrimitiveStatement(tag, funSpec, paramName)
                ?: generateSpecialStatement(tag, funSpec, type, paramName)
                ?: throw Throwable("Input type is not supported")
            FunSpec.builder(functionName)
                .addParameter(
                    ParameterSpec(
                        paramName,
                        funSpec.returnType ?: throw Throwable("Input type not supported")
                    )
                )
                .returns(name)
                .addStatement(statement)
                .build()
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

    private fun generateSpecialStatement(
        tag: String,
        funSpec: FunSpec,
        typeElement: TypeElement,
        paramName: String
    ): String? {
        return when {
            typeElement.isParcelable() -> {
                "return apply { bundle.insertParcelable(\"$tag\", $paramName)}".wrapProof()
            }
            else -> null
        }
    }

    private fun TypeElement.isParcelable(): Boolean {
        val classSymbol = this as? Symbol.ClassSymbol ?: return false
        val classType = classSymbol.type as? Type.ClassType ?: return false
        val typeSet = mutableSetOf<Type.ClassType>()
        findAllTypes(typeSet, classType)
        return typeSet.any { it.asTypeName() == PARCELABLE }
    }

    private fun findAllTypes(set: MutableSet<Type.ClassType>, type: Type.ClassType) {
        set.add(type)
        type.interfaces_field
            .filterIsInstance<Type.ClassType>()
            .forEach { findAllTypes(set, it) }

        val superType = type.supertype_field
        if (superType is Type.ClassType) {
            findAllTypes(set, superType)
        }
        return
    }
}