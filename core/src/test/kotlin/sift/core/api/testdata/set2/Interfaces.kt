package sift.core.api.testdata.set2

import sift.core.api.testdata.set1.Payload

object Interfaces {
    interface A
    interface B : A
    interface C : B
    interface D : C

    abstract class Base

    open class ImplementorA
    open class ImplementorB : Base(), A
    open class ImplementorC : ImplementorB()
    open class ImplementorD : ImplementorC(), D
}