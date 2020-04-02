package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.*
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind

/**
 * Generates the companion object of the Bunch. The methods include entry setter methods.
 */
@KotlinPoetMetadataPreview
internal class CompanionGenerator {

    /**
     * Generates the companion method type spec
     *
     * @param jProp Java properties
     * @param kProp Kotlin properties
     * @return the companion object type spec
     */
    fun generate(jProp: JavaProperties, kProp: KotlinProperties): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addFunctions(generateEntryFunction(jProp, kProp))
            .addFunction(createDuplicateFunction())
            .addFunction(crossFunction(jProp))
            .addFunctions(createSetters())
            .addFunction(createParcelableSetter())
            .addFunction(createParcelableListSetter())
            .addFunction(createSerializableSetter())
            .build()
    }

    /**
     * Generates the entye methods. It checks for both source class functions and companion object methods
     *
     * @param jProp Java properties
     * @param kProp Kotlin properties
     * @return the list of function specs that will be generated
     */
    private fun generateEntryFunction(
        jProp: JavaProperties,
        kProp: KotlinProperties
    ): List<FunSpec> {
        val companionSpecs =
            kProp.getTypeSpec().typeSpecs.firstOrNull { it.isCompanion }?.funSpecs ?: emptyList()
        return kProp.getTypeSpec().funSpecs.plus(companionSpecs)
            .asSequence()
            .map { funSpec -> pairWithJavaMethod(funSpec, jProp) }
            .filter { JavaProperties.getItemAnnotation(it.second) != null }
            .flatMap { generateSequence(it.first, it.second, jProp.getTargetTypeName()) }
            .toList()
    }

    /**
     * Pairs a kotlin method in the class and/or the companion object with its Java symbol counterpart.
     * Both are necessary because kotlin types are easier to work with in terms of type names and
     * Java symbols are necessary for type hierarchy checking.
     *
     * @param funSpec kotlin function spec
     * @param jProp Java property
     * @return the pair of kotlin and java method
     */
    private fun pairWithJavaMethod(
        funSpec: FunSpec,
        jProp: JavaProperties
    ): Pair<FunSpec, Element> {
        val enclosedElements =
            jProp.getElement().enclosedElements.filter { it.kind == ElementKind.CLASS }
                .flatMap { classes -> classes.enclosedElements.filter { it.kind == ElementKind.METHOD } }
        val jFun =
            jProp.getMethods().plus(enclosedElements)
                .firstOrNull { it.simpleName.toString() == funSpec.name }
                ?: throw ProcessorException(
                    jProp.getElement(),
                    "Some functions cannot be interpreted"
                )
        return funSpec to jFun
    }

    /**
     * Generates the setter function spec sequence. This takes into consideration the custom tags
     * and setter functions.
     *
     * @param funSpec kotlin function spec
     * @param element the method element (ElementKind.METHOD)
     * @param name the output class name
     */
    private fun generateSequence(
        funSpec: FunSpec,
        element: Element,
        name: TypeName
    ): Sequence<FunSpec> {
        val annotation = JavaProperties.getItemAnnotation(element)!!
        val paramName = if (annotation.name.isEmpty()) funSpec.name else annotation.name
        val prefixes =
            if (annotation.setters.isEmpty()) arrayOf("with") else annotation.setters

        return prefixes.asSequence().map {
            val functionName = "$it${paramName.capitalize()}"
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
                .addStatement("return ${name}(Bundle()).$functionName($paramName)".wrapProof())
                .build()
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
    private fun crossFunction(jProp: JavaProperties): FunSpec {
        return FunSpec.builder("from")
            .addParameter("bundle", BUNDLE)
            .returns(jProp.getTargetTypeName())
            .addStatement("return ${jProp.getTargetTypeName()}(bundle.duplicate())".wrapProof())
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