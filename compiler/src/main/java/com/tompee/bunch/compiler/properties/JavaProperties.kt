package com.tompee.bunch.compiler.properties

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.tompee.bunch.annotation.Bunch
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements

internal class JavaProperties(
    private val typeElement: TypeElement,
    private val elements: Elements
) {

    companion object {
        /**
         * Returns the [Bunch.Item] annotation in an element if it exists
         */
        fun getItemAnnotation(element: Element): Bunch.Item? {
            return element.getAnnotation(Bunch.Item::class.java)
        }
    }

    /**
     * Returns the underlying type element
     */
    fun getElement(): TypeElement = typeElement

    /**
     * Returns the underlying type element name
     */
    fun getElementName(): ClassName = typeElement.asClassName()

    /**
     * Returns the [Bunch] annotation instance tied to the type element
     */
    fun getBunchAnnotation(): Bunch {
        return typeElement.getAnnotation(Bunch::class.java)
    }

    /**
     * Returns the list of methods in the type element
     */
    fun getMethods(): List<Element> {
        return typeElement.enclosedElements.filter { it.kind == ElementKind.METHOD }
    }

    /**
     * Returns the class name from the current package name and the provided bunch name
     */
    fun getTargetTypeName(): ClassName {
        return ClassName(getPackageName(), getBunchAnnotation().name)
    }

    /**
     * Returns the [TypeMirror]
     */
    fun getTypeMirror(): TypeMirror {
        return typeElement.asType()
    }

    /**
     * Returns the package name of the type element
     */
    fun getPackageName(): String {
        return elements.getPackageOf(typeElement).toString()
    }
}