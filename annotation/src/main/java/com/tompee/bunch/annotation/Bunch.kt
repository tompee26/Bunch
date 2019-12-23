package com.tompee.bunch.annotation

/**
 * Marks a class as a Bunch class. A bunch is a wrapper around [Bundle] that offers custom accessor
 * and type safe methods.
 *
 * @property name bunch name. Must be unique in the package.
 */
@Retention
@Target(AnnotationTarget.CLASS)
annotation class Bunch(val name: String) {

    /**
     * Bunch item indicator. All member functions annotated with this one will have setters
     * and getters function generated.
     *
     * @property name custom name of the property. This will be translated to {setters/getters}{name}. If
     *                not provided, it is by default `with/get{functionName}` (e.g. withIndex)
     * @property tag custom key in the bundle. This is by default the function name prepended
     *               with bunch. (e.g. bunch_index)
     * @property setters setter function names. A different function is generated for each provided setter.
     *                   If not provided, default will be `with`
     * @property getters getter function names. A different function is generated for each provided getter.
     *                   If not provided, default will be `get`
     */
    @Retention
    @Target(AnnotationTarget.FUNCTION)
    annotation class Item(
        val name: String = "",
        val tag: String = "",
        val setters: Array<String> = [],
        val getters: Array<String> = []
    )
}