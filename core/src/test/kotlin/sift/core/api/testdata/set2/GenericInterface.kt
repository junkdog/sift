package sift.core.api.testdata.set2

interface GenericInterface<A, B>

class GenericInterfaceImpl : GenericInterface<String, Int>
class GenericInterfaceImpl2 : GenericInterface<String, Float>
class GenericInterfaceImpl3 : GenericInterface<String, GenericInterface<Boolean, String>>

abstract class GenericClass<T>

class ConcreteClass1 : GenericClass<String>()
class ConcreteClass2 : GenericClass<Float>(), GenericInterface<String, Float>


abstract class AbstractBaseGenerics<BASE_T>

abstract class AbstractGenerics1<T, R> : AbstractBaseGenerics<T>(), GenericInterface<T, R> {
    fun foo(t: T): R? = TODO()
}

class Generics1a : AbstractGenerics1<String, Int>()
abstract class Generics1b<U> : AbstractGenerics1<U, String>()
abstract class Generics1c<T> : AbstractGenerics1<String, T>()

open class Generics2<T2, R2> : AbstractGenerics1<T2, R2>()
open class Generics2aa<T2A, R2A> : Generics2<T2A, R2A>()
class Generics2a : Generics2aa<String, Int>()