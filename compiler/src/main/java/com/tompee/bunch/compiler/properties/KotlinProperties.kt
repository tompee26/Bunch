package com.tompee.bunch.compiler.properties

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement

/**
 * Contains the type element properties
 *
 * @property env processing environment
 * @property typeElement the input type element
 */
@KotlinPoetMetadataPreview
internal class KotlinProperties @AssistedInject constructor(
    private val env: ProcessingEnvironment,
    private val classInspector: ClassInspector,
    @Assisted private val typeElement: TypeElement
) {

    @AssistedInject.Factory
    interface Factory {
        fun create(typeElement: TypeElement): KotlinProperties
    }

    /**
     * Returns true if the annotated class is an internal class
     */
    fun isInternal(): Boolean {
        return getTypeSpec().modifiers.any { it == KModifier.INTERNAL }
    }

    /**
     * Returns the type element simple name
     */
    fun getName(): String {
        return typeElement.simpleName.toString()
    }

    /**
     * Returns the package name of the type element
     */
    fun getPackageName(): String {
        return env.elementUtils.getPackageOf(typeElement).toString()
    }

    /**
     * Returns the [TypeName]
     */
    fun getTypeName(): TypeName {
        return typeElement.asType().asTypeName()
    }

    /**
     * Returns the [TypeSpec]
     */
    fun getTypeSpec(): TypeSpec {
        return typeElement.toTypeSpec(classInspector)
    }

    /**
     * Returns the [ImmutableKmClass]
     */
    fun getKmClass(): ImmutableKmClass {
        return typeElement.toImmutableKmClass()
    }
}