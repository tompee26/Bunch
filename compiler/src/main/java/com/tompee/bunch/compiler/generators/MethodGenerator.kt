package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isEnum
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.*
import com.tompee.bunch.compiler.extensions.*
import com.tompee.bunch.compiler.properties.ElementMethod
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Generates the instance methods of the Bunch. The methods include setter methods, nullable getter methods,
 * non-nullable getter methods, throw getter methods and the collector method
 */
@KotlinPoetMetadataPreview
internal class MethodGenerator(
    private val element: TypeElement,
    private val types: Types,
    private val elements: Elements,
    bunch: Bunch,
    packageName: String
) {

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

    private val methods by lazy { ElementMethod(element).getAllInformation() }

    private val targetName = ClassName(packageName, bunch.name)

    /**
     * Generates the instance setter methods. It checks for both source class functions and companion object methods
     *
     * @return the list of function specs that will be generated
     */
    fun generateSetters(): List<FunSpec> {
        return methods.flatMap { generateSetterList(it) }
    }

    /**
     * Generates the instance getter methods. It checks for both source class functions and companion object methods
     *
     * @return the list of function specs that will be generated
     */
    fun generateGetters(): List<FunSpec> {
        return methods.flatMap { generateGetterList(it) }
    }

    /**
     * Generates the assertion methods.
     *
     * @return the list of assertion methods
     */
    fun generateAsserts(): List<FunSpec> {
        val typeName = ClassName(targetName.toString(), "Assert")
        return methods.map {
            val paramName = if (it.item.name.isEmpty()) it.kotlin.name else it.item.name
            val tag = if (it.item.tag.isEmpty()) "tag_${it.kotlin.name}" else it.item.tag

            FunSpec.builder("has${paramName.capitalize()}")
                .returns(typeName)
                .addStatement("return Assert().add(\"$tag\")")
                .build()
        }
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
     */
    private fun generateSetterList(info: ElementMethod.MethodInfo): List<FunSpec> {
        val paramName = if (info.item.name.isEmpty()) info.kotlin.name else info.item.name
        val prefixes =
            if (info.item.setters.isEmpty()) arrayOf("with") else info.item.setters
        val tag = if (info.item.tag.isEmpty()) "tag_${info.kotlin.name}" else info.item.tag
        val returnType = info.kotlin.returnType.typeName

        return prefixes.map {
            val functionName = "$it${paramName.capitalize()}"
            val statement = generateSetReturnStatement(tag, returnType, paramName, info.java)

            FunSpec.builder(functionName)
                .addParameter(ParameterSpec(paramName, returnType))
                .returns(targetName)
                .addStatement(statement)
                .build()
        }
    }

    /**
     * Generates the different assignment statement of each type
     *
     * @param tag the actual bundle tag (already considered custom tag)
     * @param typeName the function return type
     * @param param the actual parameter name (already considered custom names)
     * @param element the enclosing element for debugging purposes (ElementKind.METHOD)
     */
    private fun generateSetReturnStatement(
        tag: String,
        typeName: TypeName,
        param: String,
        element: Element
    ): String {
        return when {
            typeName in primitiveSet -> {
                "return apply { bundle.insert(\"$tag\", $param)}".wrapProof()
            }
            typeName.isParcelableList() -> {
                "return apply { bundle.insertParcelableList(\"$tag\", $param)}".wrapProof()
            }
            typeName.isEnum() -> {
                "return apply { bundle.insert(\"$tag\", $param.name)}".wrapProof()
            }
            typeName.isParcelable() -> {
                "return apply { bundle.insertParcelable(\"$tag\", $param)}".wrapProof()
            }
            typeName.isSerializable() -> {
                "return apply { bundle.insertSerializable(\"$tag\", $param)}".wrapProof()
            }
            else -> throw ProcessorException(element, "Input type is not supported")
        }
    }

    /**
     * Generates the getter function spec sequence. This takes into consideration the custom tags
     * and getter functions.
     */
    private fun generateGetterList(info: ElementMethod.MethodInfo): List<FunSpec> {
        val paramName = if (info.item.name.isEmpty()) info.kotlin.name else info.item.name
        val prefixes = if (info.item.getters.isEmpty()) arrayOf("get") else info.item.getters
        val tag = if (info.item.tag.isEmpty()) "tag_${info.kotlin.name}" else info.item.tag
        val returnType = info.kotlin.returnType.typeName

        return prefixes.flatMap {
            generateGetterFuncSpecs(
                "$it${paramName.capitalize()}",
                tag,
                returnType,
                info
            )
        }
    }

    /**
     * Generates the different getter function specs. Getter methods can be default, nullable,
     * non-nullable or throw methods
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param typeName the function return type
     * @param info method info
     */
    private fun generateGetterFuncSpecs(
        functionName: String,
        tag: String,
        typeName: TypeName,
        info: ElementMethod.MethodInfo
    ): List<FunSpec> {
        return when {
            typeName in defaultValueMap.keys ->
                createDefaultGetters(functionName, tag, typeName, info)
            typeName in nullableDefaultValueMap.keys ->
                createNullableDefaultGetters(functionName, tag, typeName, info)
            typeName in nonDefaultValueMap.keys ->
                createNonDefaultGetters(functionName, tag, typeName, info)
            typeName.isParcelableList() ->
                createParcelableArrayListGetters(functionName, tag, typeName, info)
            typeName.isEnum() ->
                createEnumGetters(functionName, tag, typeName, info)
            typeName.isParcelable() ->
                createParcelableGetters(functionName, tag, typeName, info)
            typeName.isSerializable() ->
                createSerializableGetters(functionName, tag, typeName, info)
            else -> throw ProcessorException(info.java, "Input type is not supported")
        }
    }

    /**
     * Creates the default value getters. This does not have throw getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param typeName the function return type
     * @param info method info
     */
    private fun createDefaultGetters(
        functionName: String,
        tag: String,
        typeName: TypeName,
        info: ElementMethod.MethodInfo
    ): List<FunSpec> {
        val getterName =
            defaultValueMap.entries.first { it.key == typeName }.value
        return listOf(
            if (info.isAbstract) {
                FunSpec.builder(functionName)
                    .returns(typeName)
                    .addStatement("return bundle.get$getterName(\"$tag\")".wrapProof())
                    .build()
            } else {
                FunSpec.builder(functionName)
                    .returns(typeName)
                    .addStatement("return bundle.get$getterName(\"$tag\", ${element.className}.${info.kotlin.name}())".wrapProof())
                    .build()
            }
        )
    }

    /**
     * Creates the nullable default value getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param typeName the function return type
     * @param info method info
     */
    private fun createNullableDefaultGetters(
        functionName: String,
        tag: String,
        typeName: TypeName,
        info: ElementMethod.MethodInfo
    ): List<FunSpec> {
        val getterName =
            nullableDefaultValueMap.entries.first { it.key == typeName }.value
        return if (info.isAbstract) {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(true))
                    .addStatement("return bundle.get$getterName(\"$tag\")".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.get$getterName(\"$tag\") ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.get$getterName(\"$tag\", ${element.className}.${info.kotlin.name}())".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Creates the non default value getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param typeName the function return type
     * @param info method info
     */
    private fun createNonDefaultGetters(
        functionName: String,
        tag: String,
        typeName: TypeName,
        info: ElementMethod.MethodInfo
    ): List<FunSpec> {
        val getterName =
            nonDefaultValueMap.entries.first { it.key == typeName }.value
        return if (info.isAbstract) {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(true))
                    .addStatement("return bundle.get$getterName(\"$tag\")".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.get$getterName(\"$tag\") ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.get$getterName(\"$tag\") ?: ${element.className}.${info.kotlin.name}()".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Creates the parcelable array list getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param typeName the function return type
     * @param info method info
     */
    private fun createParcelableArrayListGetters(
        functionName: String,
        tag: String,
        typeName: TypeName,
        info: ElementMethod.MethodInfo
    ): List<FunSpec> {
        return if (info.isAbstract) {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(true))
                    .addStatement("return bundle.getParcelableArrayList(\"$tag\")".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.getParcelableArrayList(\"$tag\") ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(false))
                    .addStatement(
                        "return bundle.getParcelableArrayList(\"$tag\") ?: ${element.className}.${info.kotlin.name}()".wrapProof()
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
     * @param typeName the function return type
     * @param info method info
     */
    private fun createEnumGetters(
        functionName: String,
        tag: String,
        typeName: TypeName,
        info: ElementMethod.MethodInfo
    ): List<FunSpec> {
        return if (info.isAbstract) {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(true))
                    .addStatement("return bundle.getString(\"$tag\")?.let(${typeName}::valueOf)".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.getString(\"$tag\")?.let(${typeName}::valueOf) ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.getString(\"$tag\")?.let(${typeName}::valueOf) ?: ${element.className}.${info.kotlin.name}()".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Creates the parcelable getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param typeName the function return type
     * @param info method info
     */
    private fun createParcelableGetters(
        functionName: String,
        tag: String,
        typeName: TypeName,
        info: ElementMethod.MethodInfo
    ): List<FunSpec> {
        return if (info.isAbstract) {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(true))
                    .addStatement("return bundle.getParcelable(\"$tag\")".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.getParcelable(\"$tag\") ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.getParcelable(\"$tag\") ?: ${element.className}.${info.kotlin.name}()".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Creates the serializable getters
     *
     * @param functionName the getter function name (already considered custom names)
     * @param tag the actual bundle tag (already considered custom tag)
     * @param typeName the function return type
     * @param info method info
     */
    private fun createSerializableGetters(
        functionName: String,
        tag: String,
        typeName: TypeName,
        info: ElementMethod.MethodInfo
    ): List<FunSpec> {
        return if (info.isAbstract) {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(true))
                    .addStatement("return bundle.getSerializable(\"$tag\") as? $typeName".wrapProof())
                    .build(),
                FunSpec.builder("${functionName}OrThrow")
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.getSerializable(\"$tag\") as? $typeName ?: $THROW_STATEMENT".wrapProof())
                    .build()
            )
        } else {
            listOf(
                FunSpec.builder(functionName)
                    .returns(typeName.copy(false))
                    .addStatement("return bundle.getSerializable(\"$tag\") as? $typeName ?: ${element.className}.${info.kotlin.name}()".wrapProof())
                    .build()
            )
        }
    }

    /**
     * Checks if a type is an enum
     */
    private fun TypeName.isEnum(): Boolean {
        return try {
            elements.getTypeElement(toString()).metadata.toImmutableKmClass().isEnum
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a type is a parcelable list
     */
    private fun TypeName.isParcelableList(): Boolean {
        val rawType = (this as? ParameterizedTypeName)?.rawType ?: return false
        if (rawType != LIST) return false
        val childType = this.typeArguments.firstOrNull() ?: return false
        val child = elements.getTypeElement(childType.toString()).asType()
        val parcelable = elements.getTypeElement(PARCELABLE.toString()).asType()
        return types.isAssignable(child, parcelable)
    }

    /**
     * Checks if a type is parcelable
     */
    private fun TypeName.isParcelable(): Boolean {
        val typeMirror = elements.getTypeElement(toString()).asType()
        val parcelable = elements.getTypeElement(PARCELABLE.toString()).asType()
        return types.isAssignable(typeMirror, parcelable)
    }

    /**
     * Checks if a type is serializable
     */
    private fun TypeName.isSerializable(): Boolean {
        val typeMirror = elements.getTypeElement(toString()).asType()
        val serializable = elements.getTypeElement(SERIALIZABLE.toString()).asType()
        return types.isAssignable(typeMirror, serializable)
    }
}