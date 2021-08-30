# Bunch
An Android bundle wrapper generator that offers customizable interface, type-safety and self-documentation.

Say goodbye to your bland bundles!

## What is Bunch
Bunch is wrapper over bundle that provides custom interface and type-safety. `Bunch` is a specialized bundle with custom rules. For example, you want to include only a certain group of types, say for instance, vegetables. You can restrict this `VegetableBunch` to only accept `Eggplants` and `Squash`. You can also customize the setter names to make it more self-documenting like `mixWithTomatoes` or `aPinchOfPepper`. Fun isn't it?

## Features
- Generates an implementation of a bundle wrapper with custom interface
- Conversion to and from the native `Bundle`
- Supports default values and error variants

## Getting started
Note: `kapt` is needed to process annotations

### Version 0.4.0 and above
Starting 0.4.0, all artifacts will be uploaded to Maven Central. Note that old versions will not be reuploaded anymore.

In your module's `build.gradle`, add this

```
dependencies {
    implementation "io.github.tompee26:bunch-annotations:$latest_version"
    kapt "io.github.tompee26:bunch-compiler:$latest_version"
}
```

### Versions 0.3.0
Version 0.4.0 is available in Github packages.

In your application `build.gradle`, add the repository link and authentication details. For more information, check here: https://docs.github.com/en/packages/guides/configuring-gradle-for-use-with-github-packages#authenticating-to-github-packages

```
allprojects {
    repositories {
        maven {
            url 'https://maven.pkg.github.com/tompee26/Bunch'
            credentials {
                username = "your_github_username"
                password = "your_personal_access_token"
            }
        }
    }
}

```

And in your module's `build.gradle`, add this

```
dependencies {
    compileOnly "com.tompee.bunch:annotations:$latest_version"
    kapt "com.tompee.bunch:compiler:$latest_version"
}
```

### Versions 0.2.0 and below
All versions below and including 0.2.0 are hosted in jCenter. Since bintray and jCenter are being discontinued, you will no longer be able to do this.

In your `build.gradle`, add the following dependencies:

```
dependencies {
   compileOnly "com.tompee.bunch:annotations:$latest_version"
   kapt "com.tompee.bunch:compiler:$latest_version"
}
```

Define an `abstract class` and annotate with `@Bunch` with a name. This `name` will be a new generated type so it must be unique within the package. 

```kotlin
@Bunch("Vegetables")
abstract class VegetableInfo
```

## Defining setters
To define a setter, annotate a function inside the `Bunch` class with `@Bunch.Item`. A custom `name` can be provided. When a custom name is provided, this will act as the subject in the method name. A custom `tag` can also be provided. This custom tag will be used as key in the bundle. If not provided, this will be `tag_{method_name}`. The `setters` is an array of custom setter name prefixes. A function will be generated per setter name. If not provided, a default setter prefix named `with` is generated. The function name is a combination of the setter and the name. See example below. 

```kotlin
@Bunch("Vegetables")
abstract class VegetableInfo {

    @Bunch.Item
    abstract fun pickles(): Int

    @Bunch.Item(name = "tomatoes", tag="ripe_tomatoes", setters=["withABagOf"])
    abstract fun tomatoes(): Int
}
```

Notice that the function follows a contract. The return type determines the input/output type. This is how you can use the bunch.

```kotlin
Vegetables.withABagOfTomatoes(2).collect()
```

Instance methods are also generated to allow method chaining.

```kotlin
Vegetables.withPickles(2)
    .withABagOfTomatoes(2)
    .collect()
```

Note: You have to compile the project after creating the target class to allow the processor to generate the code.

## Defining getters
Getters follow the same rules as setters and is generated along with it. `@Bunch.Item` has a `getters` property that allows you to set custom getter names.

```kotlin
@Bunch("Vegetables")
abstract class VegetableInfo {

    @Bunch.Item
    abstract fun pickles(): Int

    @Bunch.Item(name = "tomatoes", tag="ripe_tomatoes", setters=["withABagOf"], getters=["squeeze"])
    abstract fun tomatoes(): Int
}
```

```kotlin
val tomatoes = Vegetables.from(bundle).squeezeTomatoes()
```

## Supported Types
Below is the table of supported types

| Type                | Default Value | Nullable | Override Default | Has Error Variant |
|:--------------------|:--------------|:---------|:-----------------|:------------------|
| Boolean             | false         | No       | Yes              | No                |
| Byte                | 0             | No       | Yes              | No                |
| Char                | char (0)      | No       | Yes              | No                |
| Double              | 0.0           | No       | Yes              | No                |
| Float               | 0.0f          | No       | Yes              | No                |
| Int                 | 0             | No       | Yes              | No                |
| Long                | 0L            | No       | Yes              | No                |
| Short               | short (0)     | No       | Yes              | No                |
| BooleanArray        | null          | Yes      | Yes              | Yes               |
| Bundle              | null          | Yes      | Yes              | Yes               |
| ByteArray           | null          | Yes      | Yes              | Yes               |
| CharArray           | null          | Yes      | Yes              | Yes               |
| CharSequence        | null          | Yes      | Yes              | Yes               |
| Array<CharSequence> | null          | Yes      | Yes              | Yes               |
| DoubleArray         | null          | Yes      | Yes              | Yes               |
| FloatArray          | null          | Yes      | Yes              | Yes               |
| IntArray            | null          | Yes      | Yes              | Yes               |
| LongArray           | null          | Yes      | Yes              | Yes               |
| ShortArray          | null          | Yes      | Yes              | Yes               |
| StringArray         | null          | Yes      | Yes              | Yes               |
| Parcelable          | null          | Yes      | Yes              | Yes               |
| List\<Parcelable\>  | null          | Yes      | Yes              | Yes               |
| Serializable        | null          | Yes      | Yes              | Yes               |
| Enum                | null          | Yes      | Yes              | Yes               |

## Default values
To be able to provide default values, the function definition must be concrete function and returns the default value. To support this, declare a companion object and define the functions inside. 

```kotlin
@Bunch("Vegetables")
abstract class VegetableInfo {

    @Bunch.Item
    abstract fun pickles(): Int

    @Bunch.Item(name = "tomatoes", tag="ripe_tomatoes", setters=["withABagOf"], getters=["squeeze"])
    abstract fun tomatoes(): Int

    companion object {

        @Bunch.Item(setters = ["andAHalf"])
        fun cabbage() : String = "MyCabbage"
    }
}
```

The getter function `cutCabbage` now will have a non-nullable return type.

## Error variants
If a function returns a nullable type, an `orThrow` variant is also generated.

```kotlin
@Bunch("Vegetables")
abstract class VegetableInfo {

    @Bunch.Item
    abstract fun pickles(): Int

    @Bunch.Item(name = "tomatoes", tag="ripe_tomatoes", setters=["withABagOf"], getters=["squeeze"])
    abstract fun tomatoes(): Int

    @Bunch.Item(getters = ["cut"])
    abstract fun cabbage(): String
}
```

The above can be used as

```kotlin
val cabbage = Vegetables.from(bundle).cutCabbageOrThrow()
```

Contributions are welcome!

## License
```
MIT License

Copyright (c) 2019 tompee

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
