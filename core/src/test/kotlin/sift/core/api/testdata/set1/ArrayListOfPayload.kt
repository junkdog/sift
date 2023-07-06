@file:Suppress("UNUSED_PARAMETER")

package sift.core.api.testdata.set1

class ArrayListOfPayload() : HashMap<String, Payload>()


class ClassWithGenericElements {
    var payloads: List<Payload> = listOf()

    fun thisShouldNotBreakStuff(): String = "Hello World"

    fun complexReturn(): Map<String, List<Pair<Payload, Int>>> = mapOf()
}

class ClassWithGenericMethodParameters {
    fun payloads(payloadas: List<Payload>) = Unit
    fun complexParameters(map: Map<String, List<Pair<String, Int>>>) = Unit

    fun thisShouldNotBreakStuff(): String = "Hello World"

}

abstract class ClassExtendingMapOfOmgPayload() : HashMap<Omg, Payload>()

class Omg