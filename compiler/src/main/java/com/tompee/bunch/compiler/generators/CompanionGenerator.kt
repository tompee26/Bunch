package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.*
import com.tompee.bunch.compiler.extensions.capitalize
import com.tompee.bunch.compiler.extensions.typeName
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.properties.ElementMethod
import javax.lang.model.element.TypeElement

/**
 * Generates the companion object of the Bunch. The methods include entry setter methods.
 */
@KotlinPoetMetadataPreview
internal class CompanionGenerator {

    /**
     * Generates the companion method type spec
     *
     * @return the companion object type spec
     */
    fun generate(element: TypeElement, bunch: Bunch, packageName: String): TypeSpec {
        val targetTypeName = ClassName(packageName, bunch.name)
        val methods = ElementMethod(element).getAllInformation()

        return TypeSpec.companionObjectBuilder()
            .addFunctions(generateEntryFunction(targetTypeName, methods))
            .addFunction(createDuplicateFunction())
            .addFunction(crossFunction(targetTypeName))
            .addFunctions(createSetters())
            .addFunction(createParcelableSetter())
            .addFunction(createParcelableListSetter())
            .addFunction(createSerializableSetter())
            .build()
    }

    /**
     * Generates the entye methods. It checks for both source class functions and companion object methods
     *
     * @return the list of function specs that will be generated
     */
    private fun generateEntryFunction(
        targetName: ClassName,
        methods: List<ElementMethod.MethodInfo>
    ): List<FunSpec> {
        return methods.flatMap { info ->
            val paramName = if (info.item.name.isEmpty()) info.kotlin.name else info.item.name
            val prefixes =
                if (info.item.setters.isEmpty()) arrayOf("with") else info.item.setters
            prefixes.map {
                val functionName = "$it${paramName.capitalize()}"
                FunSpec.builder(functionName)
                    .addParameter(ParameterSpec(paramName, info.kotlin.returnType.typeName))
                    .returns(targetName)
                    .addStatement("return ${targetName}(Bundle()).$functionName($paramName)".wrapProof())
                    .build()
            }
        }
    }

    /**
     * Generates the duplicator function
     */
    private fun createDuplicateFunction(): FunSpec {
        return FunSpec.builder("duplicate")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .returns(BUNDLE)
            .addStatement("return if (this == Bundle.EMPTY) Bundle() else clone() as Bundle".wrapProof())
            .build()
    }

    /**
     * Generates the world crossing functions
     */
    private fun crossFunction(targetName: ClassName): FunSpec {
        return FunSpec.builder("from")
            .addParameter("bundle", BUNDLE)
            .returns(targetName)
            .addStatement("return ${targetName}(bundle.duplicate())".wrapProof())
            .build()
    }

    /**
     * Generates the primitive setter functions
     */
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

    /**
     * Generates the parcelable setter function
     */
    private fun createParcelableSetter(): FunSpec {
        return FunSpec.builder("insertParcelable")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .addParameter("tag", STRING)
            .addParameter("value", PARCELABLE)
            .addStatement("putParcelable(tag, value)".wrapProof())
            .build()
    }

    /**
     * Generates the parcelable list setter function
     */
    private fun createParcelableListSetter(): FunSpec {
        return FunSpec.builder("insertParcelableList")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .addParameter("tag", STRING)
            .addParameter("value", PARCELABLE_LIST)
            .addStatement("putParcelableArrayList(tag, ArrayList(value))".wrapProof())
            .build()
    }

    /**
     * Generates the serializable setter function
     */
    private fun createSerializableSetter(): FunSpec {
        return FunSpec.builder("insertSerializable")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .addParameter("tag", STRING)
            .addParameter("value", SERIALIZABLE)
            .addStatement("putSerializable(tag, value)".wrapProof())
            .build()
    }
}