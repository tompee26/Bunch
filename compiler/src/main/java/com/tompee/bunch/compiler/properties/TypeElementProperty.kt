package com.tompee.bunch.compiler.properties

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.tompee.bunch.annotation.Bunch
import com.tompee.bunch.compiler.ProcessorException
import com.tompee.bunch.compiler.extensions.className
import com.tompee.bunch.compiler.extensions.metadata
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@OptIn(KotlinPoetMetadataPreview::class)
internal class TypeElementProperty(
    val element: TypeElement,
    elements: Elements,
    val types: Types
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
     * Returns the [Bunch] annotation instance tied to the type element
     */
    val bunchAnnotation: Bunch = element.getAnnotation(Bunch::class.java)

    /**
     * Returns the package name of the type element
     */
    val packageName: String = elements.getPackageOf(element).toString()

    /**
     * Returns the class name from the current package name and the provided bunch name
     */
    val targetTypeName: ClassName = ClassName(packageName, bunchAnnotation.name)

    /**
     * Returns the list of methods in the type element
     */
    fun getMethods(): List<Element> {
        return element.enclosedElements.filter { it.kind == ElementKind.METHOD }
    }

    /**
     * Returns the type element's simple name
     */
    val name: String = element.simpleName.toString()

    /**
     * Returns the class inspector
     */
    val classInspector = ElementsClassInspector.create(elements, types)

    /**
     * Returns the kotlin metadata
     */
    val metadata: ImmutableKmClass = element.metadata.toImmutableKmClass()

    /**
     * Returns the [TypeName]
     */
    val className: TypeName = element.className

    /**
     * Returns true if this element is declared as private
     */
    val isPrivate: Boolean = metadata.isPrivate

    /**
     * Returns true if this element is declared as internal
     */
    val isInternal: Boolean = metadata.isInternal

    /**
     * Returns the [TypeSpec]
     */
    val typeSpec: TypeSpec = element.toTypeSpec(classInspector)

    /**
     * Returns the list of function specs and element pair
     */
    fun getFunSpecElementPairList(): List<Pair<FunSpec, Element>> {
        val companionSpec =
            typeSpec.typeSpecs.firstOrNull { it.isCompanion }?.funSpecs ?: emptyList()

        return typeSpec.funSpecs.plus(companionSpec)
            .map { pairWithJavaMethod(it) }
            .filter { getItemAnnotation(it.second) != null }
    }

    /**
     * Pairs a kotlin method in the class and/or the companion object with its Java symbol counterpart.
     * Both are necessary because kotlin types are easier to work with in terms of type names and
     * Java symbols are necessary for type hierarchy checking.
     *
     * @return the pair of kotlin and java method
     */
    private fun pairWithJavaMethod(funSpec: FunSpec): Pair<FunSpec, Element> {
        val enclosedElements =
            element.enclosedElements.filter { it.kind == ElementKind.CLASS }
                .flatMap { classes -> classes.enclosedElements.filter { it.kind == ElementKind.METHOD } }
        val jFun =
            element.enclosedElements.filter { it.kind == ElementKind.METHOD }
                .plus(enclosedElements)
                .firstOrNull { it.simpleName.toString() == funSpec.name }
                ?: throw ProcessorException(element, "Some functions cannot be interpreted")
        return funSpec to jFun
    }
}