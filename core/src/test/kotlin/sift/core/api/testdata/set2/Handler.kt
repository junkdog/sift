package sift.core.api.testdata.set2

import sift.core.api.testdata.set1.Payload

annotation class HandlerFn

class SomeFactory {
    fun create(): String = "hi"
}

class HandlerOfFns {

    @HandlerFn
    fun on(payload: Payload) {
        println(payload.c)
    }

    @HandlerFn
    fun boo(payload: Payload) {
        Payload('w')
        SomeFactory().create()
    }

    fun dummy() {
        Payload('x')
    }

    fun invoker() {
        on(Payload('a'))
        boo(Payload('b'))
    }
}