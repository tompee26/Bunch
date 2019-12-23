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
import javax.lang.model.element.ElementKind

@KotlinPoetMetadataPreview
internal class MethodGenerator @Inject constructor() {

    companion object {

        private val primitiveSet = setOf(
            BOOLEAN,
            BOOLEAN_ARRAY,
            BUNDLE,
            BYTE,
            BYTE_ARRAY,
            CHAR,
            CHAR_ARRAY,
            CHAR_SEQUENCE,
            CHAR_SEQUENCE_ARRAY,
            DOUBLE,
            DOUBLE_ARRAY,
            FLOAT,
            FLOAT_ARRAY,
            INT,
            INT_ARRAY,
            LONG,
            LONG_ARRAY,
            SHORT,
            SHORT_ARRAY,
            STRING,
            STRING_ARRAY
        )
    }

    fun generateSetters(jProp: JavaProperties, kProp: KotlinProperties): List<FunSpec> {
        val companionSpecs = kProp.getTypeSpec().typeSpecs.first { it.isCompanion }.funSpecs
        return kProp.getTypeSpec().funSpecs.plus(companionSpecs)
            .asSequence()
            .map { pairWithJavaMethod(it, jProp) }
            .filter { JavaProperties.getItemAnnotation(it.second) != null }
            .flatMap { generateSetterSequence(it.first, it.second, jProp.getTargetTypeName()) }
            .toList()
    }

    fun generateGetters(jProp: JavaProperties, kProp: KotlinProperties): List<FunSpec> {
        return kProp.getTypeSpec().funSpecs
            .asSequence()
            .map { pairWithJavaMethod(it, jProp) }
            .filter { JavaProperties.getItemAnnotation(it.second) != null }
            .flatMap { generateGetterSequence(it.first, it.second) }
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
        val enclosedElements =
            jProp.getElement().enclosedElements.filter { it.kind == ElementKind.CLASS }
                .flatMap { it.enclosedElements.filter { it.kind == ElementKind.METHOD } }
        val jFun =
            jProp.getMethods().plus(enclosedElements).firstOrNull { it.simpleName.toString() == funSpec.name }
                ?: throw ProcessorException(
                    jProp.getElement(),
                    "Some functions cannot be interpreted"
                )
        return funSpec to jFun
    }

    private fun generateSetterSequence(
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
            ?: throw ProcessorException(element, "Input type not supported")

        return prefixes.asSequence().map {
            val functionName = "$it${paramName.capitalize()}"
            val statement = generateSetReturnStatement(tag, funSpec, type, paramName, element)
            FunSpec.builder(functionName)
                .addParameter(
                    ParameterSpec(
                        paramName,
                        funSpec.returnType ?: throw ProcessorException(
                            element,
                            "Input type not supported"
                        )
                    )
                )
                .returns(name)
                .addStatement(statement)
                .build()
        }
    }

    private fun generateSetReturnStatement(
        tag: String, funSpec: FunSpec, type: Type, param: String, element: Element
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
            else -> throw ProcessorException(element, "Input type is not supported")
        }
    }

    private fun generateGetterSequence(
        funSpec: FunSpec,
        element: Element
    ): Sequence<FunSpec> {
        val annotation = JavaProperties.getItemAnnotation(element)!!
        val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
        val prefixes =
            if (annotation.getters.isEmpty()) arrayOf("get") else annotation.getters
        val tag = if (annotation.tag.isEmpty()) "tag_${funSpec.name}" else annotation.tag
        val method = element as Symbol.MethodSymbol
        val type = (method.type as? Type.MethodType)?.restype
            ?: throw ProcessorException(element, "Input type not supported")

        return prefixes.asSequence().map {
            val functionName = "$it${paramName.capitalize()}"
            val (statement, returnType) = generateGetReturnStatement(
                tag,
                funSpec,
                type,
                element
            )
            FunSpec.builder(functionName)
                .returns(returnType)
                .addStatement(statement)
                .build()
        }
    }

    private fun generateGetReturnStatement(
        tag: String, funSpec: FunSpec, type: Type, element: Element
    ): Pair<String, TypeName> {
        return when {
            // TODO: Handle default values
            funSpec.returnType in defaultValueMap.keys -> {
                val getterName =
                    defaultValueMap.entries.first { it.key == funSpec.returnType }.value
                "return bundle.get$getterName(\"$tag\")".wrapProof() to funSpec.returnType!!
            }
            funSpec.returnType in nonDefaultValueMap.keys -> {
                // TODO: Handle default values
                val getterName =
                    nonDefaultValueMap.entries.first { it.key == funSpec.returnType }.value
                "return bundle.get$getterName(\"$tag\")".wrapProof() to
                        funSpec.returnType!!.copy(true)
            }
            type.isParcelableList() -> {
                // TODO: Handle default values
                "return bundle.getParcelableArrayList(\"$tag\")".wrapProof() to
                        funSpec.returnType!!.copy(true)
            }
            type.isEnum() -> {
                // TODO: Handle default values
                "return bundle.getString(\"$tag\")?.let(${funSpec.returnType}::valueOf)".wrapProof() to
                        funSpec.returnType!!.copy(true)
            }
            type.isParcelable() -> {
                // TODO: Handle default values
                "return bundle.getParcelable(\"$tag\")".wrapProof() to
                        funSpec.returnType!!.copy(true)
            }
            type.isSerializable() -> {
                // TODO: Handle default values
                "return bundle.getSerializable(\"$tag\") as? ${funSpec.returnType}".wrapProof() to
                        funSpec.returnType!!.copy(true)
            }
            else -> throw ProcessorException(element, "Input type is not supported")
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