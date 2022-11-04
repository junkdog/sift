package sift.core.api.testdata.set1

class MethodsWithTypes {

    fun presentTypes(foo: Foo, bar: Bar) = Unit
    fun mixedTypes(foo: Foo, s: String) = Unit
    fun noPresentTypes(i: Int, s: String) = Unit

    class Foo
    class Bar
}

