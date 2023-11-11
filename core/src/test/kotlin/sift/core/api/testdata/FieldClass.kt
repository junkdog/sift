package sift.core.api.testdata

class PayLoadAbc

class FieldClass {
    val payloads: List<PayLoadAbc> = listOf()
}

@JvmInline
value class Hello(val s: String)

class KotlinClass1(
    val greetings: List<Hello>
) {
    fun Any.hi(greeting: Hello) = Unit
}

class KotlinClass2 : InterfaceWithDefaultMethod

interface InterfaceWithDefaultMethod {
    fun foo(): Unit = Unit
}


class FooImplExt : FooIfaceExt<String, Int> {
    override fun <S : String> yo(entity: S): S = TODO()
    override fun <S : String> save(entity: S): S = TODO()
    override fun <S : String> saveAll(entities: Iterable<S>?): Iterable<S> = TODO()
    override fun findById(id: Int): String? = TODO()
}

interface FooIfaceExt<T, ID> : FooIface<T, ID> {
    fun <S : T> yo(entity: S): S
}

interface FooIface<T, ID> {
    fun <S : T> save(entity: S): S
    fun <S : T> saveAll(entities: Iterable<S>?): Iterable<S>
    fun findById(id: ID): T?
}