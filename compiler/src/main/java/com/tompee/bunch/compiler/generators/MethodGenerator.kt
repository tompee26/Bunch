package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.tompee.bunch.compiler.*
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
import javax.inject.Inject
import javax.lang.model.element.Element

@KotlinPoetMetadataPreview
internal class MethodGenerator @Inject constructor() {

    fun generateSetters(jProp: JavaProperties, kProp: KotlinProperties): List<FunSpec> {
        return kProp.getTypeSpec().funSpecs
            .asSequence()
            .map { funSpec -> pairWithJavaMethod(funSpec, jProp) }
            .filter { JavaProperties.getItemAnnotation(it.second) != null }
            .flatMap { generateSequence(it.first, it.second, jProp.getTargetTypeName()) }
            .toList()
    }

    fun generateCollector(): FunSpec {
        return FunSpec.builder("collect")
            .returns(BUNDLE)
            .addStatement("return bundle".wrapProof())
            .build()
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
        val type = (method.type as? Type.MethodType)?.restype
            ?: throw Throwable("Input type not supported")

        return prefixes.asSequence().map {
            val functionName = "$it${paramName.capitalize()}"
            val statement = generateStatement(tag, funSpec, type, paramName)
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

    private fun generateStatement(
        tag: String, funSpec: FunSpec, type: Type, param: String
    ): String {
        return when {
            funSpec.returnType in primitiveSet -> {
                "return apply { bundle.insert(\"$tag\", $param)}".wrapProof()
            }
            type.isParcelableList() -> {
                "return apply { bundle.insertParcelableList(\"$tag\", $param)}".wrapProof()
            }
            type.isEnum() -> {
                "return apply { bundle.insert(\"$tag\", $param.name)}".wrapProof()
            }
            type.isParcelable() -> {
                "return apply { bundle.insertParcelable(\"$tag\", $param)}".wrapProof()
            }
            type.isSerializable() -> {
                "return apply { bundle.insertSerializable(\"$tag\", $param)}".wrapProof()
            }
            else -> throw Throwable("Input type is not supported")
        }
    }

    private fun Type.isEnum(): Boolean {
        val classType = this as? Type.ClassType ?: return false
        val superType = classType.supertype_field as? Type.ClassType ?: return false
        val name = (superType.asTypeName() as? ParameterizedTypeName)?.rawType ?: return false
        return name == JAVA_ENUM
    }

    private fun Type.isParcelableList(): Boolean {
        val classType = this as? Type.ClassType ?: return false
        val name = (classType.asTypeName() as? ParameterizedTypeName)?.rawType ?: return false
        if (name != JAVA_LIST) return false
        val typeParam = classType.typarams_field.first() as? Type.ClassType ?: return false
        val typeSet = mutableSetOf<Type.ClassType>()
        findAllTypes(typeSet, typeParam)
        return typeSet.any { it.asTypeName() == PARCELABLE }
    }

    private fun Type.isParcelable(): Boolean {
        val classType = this as? Type.ClassType ?: return false
        val typeSet = mutableSetOf<Type.ClassType>()
        findAllTypes(typeSet, classType)
        return typeSet.any { it.asTypeName() == PARCELABLE }
    }

    private fun Type.isSerializable(): Boolean {
        val classType = this as? Type.ClassType ?: return false
        val typeSet = mutableSetOf<Type.ClassType>()
        findAllTypes(typeSet, classType)
        return typeSet.any { it.asTypeName() == SERIALIZABLE }
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