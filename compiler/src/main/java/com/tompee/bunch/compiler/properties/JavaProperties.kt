package com.tompee.bunch.compiler.properties

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.kotlinpoet.ClassName
import com.tompee.bunch.annotation.Bunch
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

internal class JavaProperties @AssistedInject constructor(
    private val env: ProcessingEnvironment,
    @Assisted private val typeElement: TypeElement
) {

    companion object {
        /**
         * Returns the [Bunch.Item] annotation in an element if it exists
         */
        fun getItemAnnotation(element: Element): Bunch.Item? {
            return element.getAnnotation(Bunch.Item::class.java)
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(typeElement: TypeElement): JavaProperties
    }

    /**
     * Returns the underlying type element
     */
    fun getElement(): TypeElement = typeElement

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
        return env.elementUtils.getPackageOf(typeElement).toString()
    }
}