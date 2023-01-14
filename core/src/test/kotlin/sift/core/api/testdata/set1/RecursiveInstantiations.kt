@file:Suppress("UNUSED_PARAMETER")

package sift.core.api.testdata.set1

class RecursiveInstantiations {

    @Handler
    fun hi(command: Greeting) {
        hi(Greeting())
    }
}

class Greeting