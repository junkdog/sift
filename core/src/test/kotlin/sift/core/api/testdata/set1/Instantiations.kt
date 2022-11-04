package sift.core.api.testdata.set1


class Instantiations {


    fun caseA() {
        Yolo().hmm()
    }

    fun caseB() {
        Yolo().hmm()
        Instantiations2.a()
    }


    class Yolo {
        fun hmm() {
            Payload('h')
        }
    }
}

object Instantiations2 {
    fun a() {
        b()
        Payload('a')
    }

    fun b() {
        c()
    }

    fun c() {
        Payload('c')
    }

    fun notCalled() = Unit
}

class Payload(val c: Char)
