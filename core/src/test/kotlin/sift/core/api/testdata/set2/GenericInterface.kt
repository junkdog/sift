package sift.core.api.testdata.set2

interface GenericInterface<A, B>

class GenericInterfaceImpl : GenericInterface<String, Int>
class GenericInterfaceImpl2 : GenericInterface<String, Float>
class GenericInterfaceImpl3 : GenericInterface<String, GenericInterface<Boolean, String>>

abstract class GenericClass<T>

class ConcreteClass1 : GenericClass<String>()
class ConcreteClass2 : GenericClass<Float>(), GenericInterface<String, Float>


abstract class AbstractGenerics1<T, R> {
    fun hello(t: T): R? {
        return t.toString() as? R
    }
}

class Generics1a : AbstractGenerics1<String, Int>()
abstract class Generics1b<U> : AbstractGenerics1<U, String>()
abstract class Generics1c<T> : AbstractGenerics1<String, T>()