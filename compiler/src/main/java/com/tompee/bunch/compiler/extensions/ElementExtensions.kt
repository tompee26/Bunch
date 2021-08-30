package com.tompee.bunch.compiler.extensions

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isInternal
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.util.Elements

/**
 * Parses a given annotation from the element
 */
internal inline fun <reified T : Annotation> Element.parseAnnotation(): T? =
    getAnnotation(T::class.java)

/**
 * Returns the package name where the element belongs to
 */
internal fun Element.getPackageName(elements: Elements): String {
    return elements.getPackageOf(this).toString()
}

/**
 * Checks if this element is defined as internal
 */
@KotlinPoetMetadataPreview
internal val Element.isInternal: Boolean
    get() = try {
        metadata.toImmutableKmClass().isInternal
    } catch (e: Exception) {
        false
    }

/**
 * Returns the Kotlin metadata associated to the element
 */
internal val Element.metadata: Metadata
    get() = getAnnotation(Metadata::class.java) ?: throw IllegalStateException("No metadata found")

/**
 * Returns the class name derived from Kotlin metadata
 */
@KotlinPoetMetadataPreview
internal val Element.className: ClassName
    get() = metadata.toImmutableKmClass().let { ClassInspectorUtil.createClassName(it.name) }

@KotlinPoetMetadataPreview
internal val Element.companionObjectElement: Element?
    get() {
        return try {
            val companionObject = metadata.toImmutableKmClass().companionObject
            if (companionObject != null) {
                enclosedElements.first { it.kind == ElementKind.CLASS && it.simpleName.toString() == companionObject }
            } else null
        } catch (e: Exception) {
            null
        }
    }