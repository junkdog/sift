package sift.core.api.testdata.set1

import kotlin.reflect.KClass

annotation class RestController
annotation class Endpoint(val path: String, val method: String)
annotation class Handler
annotation class Query

enum class Yolo { Foo, Bar }

annotation class AnnoType(
    val yolo: Yolo,
    val cls: KClass<*>
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