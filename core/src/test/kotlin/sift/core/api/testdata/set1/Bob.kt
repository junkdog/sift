package sift.core.api.testdata.set1

enum class Bob {
    A,
    B,
    C
}

class Bobber {
    fun a() = Bob.A
    fun b() = Bob.A
    fun c() = listOf(Bob.A, Bob.B)
}

class Dibbler {
    fun a() = Dob.a
    fun b() = Dob.b
    fun c() = listOf(Dob.a, Dob.b)
}

object Dob {
    var a: String = "a"
    var b: String = "b"
    var c: String = "c"
}