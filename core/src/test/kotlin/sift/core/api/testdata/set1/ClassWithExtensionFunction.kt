@file:Suppress("UNUSED_PARAMETER")

package sift.core.api.testdata.set1

class ClassWithExtensionFunction {
    val foo // should not show up under methods(declared) (default)
        get() = MethodsWithTypes.Foo()

    fun List<Foo>.hello(foo: Foo): String = TODO()

    class Foo
}

