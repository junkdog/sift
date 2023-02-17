package sift.core.api.testdata.set2

object Interfaces {
    interface A
    interface B : A
    interface C : B
    interface D : C

    interface E : B, C, D

    abstract class Base

    open class ImplementorA
    open class ImplementorB : Base(), A
    open class ImplementorC : ImplementorB()
    open class ImplementorD : ImplementorC(), D

    open class ImplAx : A
    open class ImplBx : B, ImplAx()
    open class ImplCx : C, ImplBx()
    open class ImplDx : D, ImplCx()
}