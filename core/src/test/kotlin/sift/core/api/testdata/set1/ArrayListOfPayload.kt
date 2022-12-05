package sift.core.api.testdata.set1

class ArrayListOfPayload() : HashMap<String, Payload>()


class ClassWithGenericElements {
    var payloads: List<Payload> = listOf()

    fun thisShouldNotBreakStuff(): String = "Hello World"

    fun complexReturn(): Map<String, List<Pair<Payload, Int>>> = mapOf()
}


abstract class ClassExtendingMapOfOmgPayload() : HashMap<Omg, Payload>()

class Omg