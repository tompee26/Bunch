package com.tompee.bunch.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.bunch.compiler.*
import com.tompee.bunch.compiler.extensions.wrapProof
import com.tompee.bunch.compiler.properties.JavaProperties
import com.tompee.bunch.compiler.properties.KotlinProperties
import javax.annotation.processing.Messager
import javax.inject.Inject
import javax.lang.model.element.Element

@KotlinPoetMetadataPreview
internal class CompanionGenerator @Inject constructor(private val messager: Messager) {

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

    private fun generateEntryFunction(
        jProp: JavaProperties,
        kProp: KotlinProperties
    ): List<FunSpec> {
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
            ?: throw ProcessorException(jProp.getElement(), "Some functions cannot be interpreted")
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

    private fun createDuplicateFunction(): FunSpec {
        return FunSpec.builder("duplicate")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .returns(BUNDLE)
            .addStatement("return if (this == Bundle.EMPTY) Bundle() else clone() as Bundle".wrapProof())
            .build()
    }

    private fun crossFunction(jProp: JavaProperties): FunSpec {
        return FunSpec.builder("from")
            .addParameter("bundle", BUNDLE)
            .returns(jProp.getTargetTypeName())
            .addStatement("return ${jProp.getTargetTypeName()}(bundle.duplicate())".wrapProof())
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

    private fun createParcelableSetter(): FunSpec {
        return FunSpec.builder("insertParcelable")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .addParameter("tag", STRING)
            .addParameter("value", PARCELABLE)
            .addStatement("putParcelable(tag, value)".wrapProof())
            .build()
    }

    private fun createParcelableListSetter(): FunSpec {
        return FunSpec.builder("insertParcelableList")
            .addModifiers(KModifier.PRIVATE)
            .receiver(BUNDLE)
            .addParameter("tag", STRING)
            .addParameter("value", PARCELABLE_LIST)
            .addStatement("putParcelableArrayList(tag, ArrayList(value))".wrapProof())
            .build()
    }

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