package sift.core.api.testdata.set2

interface GenericInterface<A, B>

class GenericInterfaceImpl : GenericInterface<String, Int>
class GenericInterfaceImpl2 : GenericInterface<String, Float>
class GenericInterfaceImpl3 : GenericInterface<String, GenericInterface<Boolean, String>>

abstract class GenericClass<T>
class ConcreteClass1 : GenericClass<String>()
class ConcreteClass2 : GenericClass<Float>(), GenericInterface<String, Float>