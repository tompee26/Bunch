package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.tompee.bunch.compiler.*
import com.tompee.bunch.compiler.extensions.capitalize
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.properties.TypeElementProperty
import javax.lang.model.element.Element

/**
 * Generates the instance methods of the Bunch. The methods include setter methods, nullable getter methods,
 * non-nullable getter methods, throw getter methods and the collector method
 */
@KotlinPoetMetadataPreview
internal class MethodGenerator {

    companion object {

        /**
         * Default item not found exception
         */
        private const val THROW_STATEMENT = "throw IllegalStateException(\"Item not found\")"

        /**
         * These are the types with that is supported by bundle directly
         */
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

    /**
     * Generates the instance setter methods. It checks for both source class functions and companion object methods
     *
     * @return the list of function specs that will be generated
     */
    fun generateSetters(prop: TypeElementProperty): List<FunSpec> {
        return prop.getFunSpecElementPairList()
            .flatMap { generateSetterSequence(it.first, it.second, prop.targetTypeName) }
            .toList()
    }

    /**
     * Generates the instance getter methods. It checks for both source class functions and companion object methods
     *
     * @return the list of function specs that will be generated
     */
    fun generateGetters(prop: TypeElementProperty): List<FunSpec> {
        return prop.getFunSpecElementPairList()
            .flatMap {
                generateGetterSequence(prop.className, it.first, it.second)
            }
            .toList()
    }

    /**
     * Generates the assertion methods.
     *
     * @return the list of assertion methods
     */
    fun generateAsserts(prop: TypeElementProperty): List<FunSpec> {
        val typeName = ClassName("${prop.targetTypeName}", "Assert")

        return prop.getFunSpecElementPairList()
            .map {
                val (funSpec, element) = it
                val annotation = TypeElementProperty.getItemAnnotation(element)!!
                val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
                val tag = if (annotation.tag.isEmpty()) "tag_${funSpec.name}" else annotation.tag

                FunSpec.builder("has${paramName.capitalize()}")
                    .returns(typeName)
                    .addStatement("return Assert().add(\"$tag\")")
                    .build()
            }
            .toList()
    }

    /**
     * Generates the collector method. The collector method exposes the internal bundle.
     *
     * @return the collector method function spec
     */
    fun generateCollector(): FunSpec {
        return FunSpec.builder("collect")
            .returns(BUNDLE)
            .addStatement("return bundle".wrapProof())
            .build()
    }

    /**
     * Generates the setter function spec sequence. This takes into consideration the custom tags
     * and setter functions.
     *
     * @param funSpec kotlin function spec
     * @param element the method element (ElementKind.METHOD)
     * @param name the output class name
     */
    private fun generateSetterSequence(
        funSpec: FunSpec,
        element: Element,
        name: TypeName
    ): Sequence<FunSpec> {
        val annotation = TypeElementProperty.getItemAnnotation(element)!!
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

    /**
     * Generates the different assignment statement of each type
     *
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param type the parameter type
     * @param param the actual parameter name (already considered custom names)
     * @param element the enclosing element for debugging purposes (ElementKind.METHOD)
     */
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

    /**
     * Generates the getter function spec sequence. This takes into consideration the custom tags
     * and getter functions.
     *
     * @param sourceName the source class name. This is for invoking default value methods
     * @param funSpec kotlin function spec
     * @param element the method element (ElementKind.METHOD)
     */
    private fun generateGetterSequence(
        sourceName: TypeName,
        funSpec: FunSpec,
        element: Element,
    ): Sequence<FunSpec> {
        val annotation = TypeElementProperty.getItemAnnotation(element)!!
        val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
        val prefixes =
            if (annotation.getters.isEmpty()) arrayOf("get") else annotation.getters
        val tag = if (annotation.tag.isEmpty()) "tag_${funSpec.name}" else annotation.tag

        val method = element as Symbol.MethodSymbol
        val type = (method.type as? Type.MethodType)?.restype
            ?: throw ProcessorException(element, "Input type not supported")

        return prefixes.asSequence().flatMap {
            generateGetterFuncSpecs(
                "$it${paramName.capitalize()}",
                tag,
                funSpec,
                type,
                sourceName,
                element
            )
        }
    }

    /**
     * Generates the different getter function specs. Getter methods can be default, nullable,
     * non-nullable or throw methods
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param type the parameter type
     * @param sourceName the source class name. This is for invoking default value methods
     * @param element the enclosing element for debugging purposes (ElementKind.METHOD)
     */
    private fun generateGetterFuncSpecs(
        functionName: String,
        tag: String,
        funSpec: FunSpec,
        type: Type,
        sourceName: TypeName,
        element: Element
    ): Sequence<FunSpec> {
        return when {
            funSpec.returnType in defaultValueMap.keys ->
                createDefaultGetters(functionName, tag, funSpec, sourceName)
            funSpec.returnType in nullableDefaultValueMap.keys ->
                createNullableDefaultGetters(functionName, tag, funSpec, sourceName)
            funSpec.returnType in nonDefaultValueMap.keys ->
                createNonDefaultGetters(functionName, tag, funSpec, sourceName)
            type.isParcelableList() ->
                createParcelableArrayListGetters(functionName, tag, funSpec, sourceName)
            type.isEnum() ->
                createEnumGetters(functionName, tag, funSpec, sourceName)
            type.isParcelable() ->
                createParcelableGetters(functionName, tag, funSpec, sourceName)
            type.isSerializable() ->
                createSerializableGetters(functionName, tag, funSpec, sourceName)
            else -> throw ProcessorException(element, "Input type is not supported")
        }
    }

    /**
     * Creates the default value getters. This does not have throw getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param sourceName the source class name. This is for invoking default value methods
     */
    private fun createDefaultGetters(
        functionName: String,
        tag: String,
        funSpec: FunSpec,
        sourceName: TypeName
    ): Sequence<FunSpec> {
        val getterName =
            defaultValueMap.entries.first { it.key == funSpec.returnType }.value
        val defaultSpecs = if (funSpec.modifiers.any { it == KModifier.ABSTRACT }) {
            FunSpec.builder(functionName)
                .returns(funSpec.returnType!!)
                .addStatement("return bundle.get$getterName(\"$tag\")".wrapProof())
                .build()
        } else {
            FunSpec.builder(functionName)
                .returns(funSpec.returnType!!)
                .addStatement("return bundle.get$getterName(\"$tag\", ${sourceName}.${funSpec.name}())".wrapProof())
                .build()
        }
        return sequenceOf(defaultSpecs)
    }

    /**
     * Creates the nullable default value getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param sourceName the source class name. This is for invoking default value methods
     */
    private fun createNullableDefaultGetters(
        functionName: String,
        tag: String,
        funSpec: FunSpec,
        sourceName: TypeName
    ): Sequence<FunSpec> {
        val getterName =
            nullableDefaultValueMap.entries.first { it.key == funSpec.returnType }.value

        return if (funSpec.modifiers.any { it == KModifier.ABSTRACT }) {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(true))
                    .addStatement("return bundle.get$getterName(\"$tag\")".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.get$getterName(\"$tag\") ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.get$getterName(\"$tag\", ${sourceName}.${funSpec.name}())".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Creates the non default value getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param sourceName the source class name. This is for invoking default value methods
     */
    private fun createNonDefaultGetters(
        functionName: String,
        tag: String,
        funSpec: FunSpec,
        sourceName: TypeName
    ): Sequence<FunSpec> {

        val getterName =
            nonDefaultValueMap.entries.first { it.key == funSpec.returnType }.value

        return if (funSpec.modifiers.any { it == KModifier.ABSTRACT }) {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(true))
                    .addStatement("return bundle.get$getterName(\"$tag\")".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.get$getterName(\"$tag\") ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.get$getterName(\"$tag\") ?: ${sourceName}.${funSpec.name}()".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Creates the parcelable array list getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param sourceName the source class name. This is for invoking default value methods
     */
    private fun createParcelableArrayListGetters(
        functionName: String,
        tag: String,
        funSpec: FunSpec,
        sourceName: TypeName
    ): Sequence<FunSpec> {

        return if (funSpec.modifiers.any { it == KModifier.ABSTRACT }) {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(true))
                    .addStatement("return bundle.getParcelableArrayList(\"$tag\")".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.getParcelableArrayList(\"$tag\") ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement(
                        "return bundle.getParcelableArrayList(\"$tag\") ?: ${sourceName}.${funSpec.name}()".wrapProof()
                            .wrapProof()
                    )
                    .build()
            )
        }
    }

    /**
     * Creates the enum getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param sourceName the source class name. This is for invoking default value methods
     */
    private fun createEnumGetters(
        functionName: String,
        tag: String,
        funSpec: FunSpec,
        sourceName: TypeName
    ): Sequence<FunSpec> {
        return if (funSpec.modifiers.any { it == KModifier.ABSTRACT }) {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(true))
                    .addStatement("return bundle.getString(\"$tag\")?.let(${funSpec.returnType}::valueOf)".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.getString(\"$tag\")?.let(${funSpec.returnType}::valueOf) ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.getString(\"$tag\")?.let(${funSpec.returnType}::valueOf) ?: ${sourceName}.${funSpec.name}()".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Creates the parcelable getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param sourceName the source class name. This is for invoking default value methods
     */
    private fun createParcelableGetters(
        functionName: String,
        tag: String,
        funSpec: FunSpec,
        sourceName: TypeName
    ): Sequence<FunSpec> {
        return if (funSpec.modifiers.any { it == KModifier.ABSTRACT }) {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(true))
                    .addStatement("return bundle.getParcelable(\"$tag\")".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.getParcelable(\"$tag\") ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.getParcelable(\"$tag\") ?: ${sourceName}.${funSpec.name}()".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Creates the serializable getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param funSpec kotlin function spec
     * @param sourceName the source class name. This is for invoking default value methods
     */
    private fun createSerializableGetters(
        functionName: String,
        tag: String,
        funSpec: FunSpec,
        sourceName: TypeName
    ): Sequence<FunSpec> {

        return if (funSpec.modifiers.any { it == KModifier.ABSTRACT }) {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(true))
                    .addStatement("return bundle.getSerializable(\"$tag\") as? ${funSpec.returnType}".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.getSerializable(\"$tag\") as? ${funSpec.returnType} ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            sequenceOf(
                FunSpec.builder(functionName)
                    .returns(funSpec.returnType!!.copy(false))
                    .addStatement("return bundle.getSerializable(\"$tag\") as? ${funSpec.returnType} ?: ${sourceName}.${funSpec.name}()".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Checks if a type is an enum
     */
    private fun Type.isEnum(): Boolean {
        val classType = this as? Type.ClassType ?: return false
        val superType = classType.supertype_field as? Type.ClassType ?: return false
        val name = (superType.asTypeName() as? ParameterizedTypeName)?.rawType ?: return false
        return name == JAVA_ENUM
    }

    /**
     * Checks if a type is a parcelable list
     */
    private fun Type.isParcelableList(): Boolean {
        val classType = this as? Type.ClassType ?: return false
        val name = (classType.asTypeName() as? ParameterizedTypeName)?.rawType ?: return false
        if (name != JAVA_LIST) return false
        val typeParam = classType.typarams_field.first() as? Type.ClassType ?: return false
        val typeSet = mutableSetOf<Type.ClassType>()
        findAllTypes(typeSet, typeParam)
        return typeSet.any { it.asTypeName() == PARCELABLE }
    }

    /**
     * Checks if a type is parcelable
     */
    private fun Type.isParcelable(): Boolean {
        val classType = this as? Type.ClassType ?: return false
        val typeSet = mutableSetOf<Type.ClassType>()
        findAllTypes(typeSet, classType)
        return typeSet.any { it.asTypeName() == PARCELABLE }
    }

    /**
     * Checks if a type is serializable
     */
    private fun Type.isSerializable(): Boolean {
        val classType = this as? Type.ClassType ?: return false
        val typeSet = mutableSetOf<Type.ClassType>()
        findAllTypes(typeSet, classType)
        return typeSet.any { it.asTypeName() == SERIALIZABLE }
    }

    /**
     * Aggregates all super types of a single type
     */
    private fun findAllTypes(set: MutableSet<Type.ClassType>, type: Type.ClassType) {
        set.add(type)
        type.interfaces_field
            ?.filterIsInstance<Type.ClassType>()
            ?.forEach { findAllTypes(set, it) }

        val superType = type.supertype_field
        if (superType is Type.ClassType) {
            findAllTypes(set, superType)
        }
        return
    }
}