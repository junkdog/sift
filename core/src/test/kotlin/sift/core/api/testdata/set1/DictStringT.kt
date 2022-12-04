package sift.core.api.testdata.set1

abstract class DictStringT<T> : HashMap<String, T>() {

    val listOfPayload = listOf<Payload>()
    val listOfT = listOf<T>()
}
