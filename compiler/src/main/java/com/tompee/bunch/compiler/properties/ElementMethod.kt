package com.tompee.bunch.compiler.properties

import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isAbstract
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.extensions.companionObjectElement
import com.tompee.bunch.compiler.extensions.metadata
import com.tompee.bunch.compiler.extensions.parseAnnotation
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

/**
 * A utility class that loads the companion and regular methods
 */
@KotlinPoetMetadataPreview
internal class ElementMethod(private val element: Element) {

    data class MethodInfo(
        val kotlin: ImmutableKmFunction,
        val java: ExecutableElement,
        val item: Bunch.Item,
        val isAbstract: Boolean
    )

    /**
     * Caches all the companion methods available in the element
     */
    private val kotlinCompanionMethods by lazy {
        element.companionObjectElement
            ?.metadata?.toImmutableKmClass()
            ?.functions
            ?: emptyList()
    }

    /**
     * Caches all the java companion methods that are properly annotated
     */
    private val javaCompanionMethods by lazy {
        element.companionObjectElement
            ?.enclosedElements
            ?.filter { it.kind == ElementKind.METHOD }
            ?.filter { it.parseAnnotation<Bunch.Item>() != null }
            ?.map { it as ExecutableElement }
            ?: emptyList()
    }

    /**
     * Caches all the regular kotlin methods
     */
    private val kotlinMethods by lazy {
        element.metadata.toImmutableKmClass().functions
    }

    /**
     * Caches all the regular java methods that are properly annotated
     */
    private val javaMethods by lazy {
        element.enclosedElements
            .filter { it.kind == ElementKind.METHOD }
            .filter { it.parseAnnotation<Bunch.Item>() != null }
            .map { it as ExecutableElement }
    }

    /**
     * Returns a list of java and kotlin method representation
     */
    fun getAllInformation(): List<MethodInfo> {
        return javaCompanionMethods.map { jm ->
            // There is a limitation here. Since we are only looking at the function name,
            // if there are static overloads, we might not be able to get the correct one
            val kotlinMethod = kotlinCompanionMethods.first { it.name == jm.simpleName.toString() }
            val annotation = jm.parseAnnotation<Bunch.Item>()!!
            MethodInfo(kotlinMethod, jm, annotation, kotlinMethod.isAbstract)
        }.plus(
            javaMethods.map { jm ->
                // There is a limitation here. Since we are only looking at the function name,
                // if there are static overloads, we might not be able to get the correct one
                val kotlinMethod =
                    kotlinMethods.first { it.name == jm.simpleName.toString() }
                val annotation = jm.parseAnnotation<Bunch.Item>()!!
                MethodInfo(kotlinMethod, jm, annotation, kotlinMethod.isAbstract)
            }
        )
    }
}