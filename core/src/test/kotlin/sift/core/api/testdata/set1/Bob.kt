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