package sift.core.api.testdata.set1

class ClassWithGenericTypes<T> {
    var mapOfStringInt = mutableMapOf<String, Int>()

    var doubleList: List<Double> = listOf()

    var genericField: T? = null
}

