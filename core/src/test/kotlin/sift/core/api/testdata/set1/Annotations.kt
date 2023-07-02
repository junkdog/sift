package sift.core.api.testdata.set1

import kotlin.reflect.KClass

annotation class RestController(val values: Array<String> = [])
annotation class Endpoint(val path: String, val method: String)
annotation class Handler
annotation class Query

enum class Yolo { Foo, Bar }

annotation class AnnoType(
    val value: Int,
    val cls: KClass<*>
)

annotation class NodeAnno(
    val value: Int,
    val children: Array<NodeAnno>,
)

annotation class AnnoPrimitives(
    val bool: Boolean,
    val byte: Byte,
    val char: Char,
    val short: Short,
    val int: Int,
    val long: Long,
    val float: Float,
    val double: Double,
)

annotation class AnnoWithClasses(
    val types: Array<KClass<*>>,
)

annotation class NestingAnno(
    val foos: Array<AnnoWithClasses>,
    val bars: Array<AnnoWithClasses>,
)

annotation class DeepNestingAnno(
    val cls: KClass<*>,
    val root: NestingAnno,
)