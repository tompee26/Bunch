package com.tompee.bunch.annotation

/**
 * Marks a class as a Bunch class. A bunch is a wrapper around [Bundle] that offers custom accessor
 * and type safe methods.
 *
 * @property name bunch name. This will be a type alias of [Bundle].
 * @property setters the default setter function name. By default it will contain a single item named `with`
 */
@Target(AnnotationTarget.CLASS)
@Retention
annotation class Bunch(val name: String, val setters: Array<String> = [])