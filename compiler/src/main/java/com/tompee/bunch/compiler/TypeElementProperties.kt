package com.tompee.bunch.compiler

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.bunch.annotation.Bunch
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

/**
 * Contains the type element properties
 *
 * @property env processing environment
 * @property typeElement the input type element
 */
@KotlinPoetMetadataPreview
internal class TypeElementProperties(
    private val env: ProcessingEnvironment,
    private val typeElement: TypeElement
) {
    private val classInspector = ElementsClassInspector.create(env.elementUtils, env.typeUtils)

    /**
     * Returns the [Bunch] annotation instance tied to the type element
     */
    fun getBunchAnnotation(): Bunch {
        return typeElement.getAnnotation(Bunch::class.java)
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
     * Returns the [TypeMirror]
     */
    fun getTypeMirror(): TypeMirror {
        return typeElement.asType()
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