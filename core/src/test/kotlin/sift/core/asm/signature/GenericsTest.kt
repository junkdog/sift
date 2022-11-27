package sift.core.asm.signature

import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.util.TraceSignatureVisitor
import sift.core.asm.*

class GenericsTest {
    @Test
    fun `read simple class signature`() {
//        val cn = classNode(ClassExtendingAnyAny::class)
        listOf(
//            classNode(ClassExtendingMap::class),
//            classNode(ClassExtendingList::class),
//            classNode(ClassExtendingListMap::class),
//            classNode(ClassExtendingAnyAny2::class),
            classNode(InterfaceA::class),
        ).forEach { cn ->

            val sp = SignatureParser(Opcodes.ASM9, LoggingSignatureVisitor(Opcodes.ASM9))
            val trace = TraceSignatureVisitor(0)

            println(cn.type.simpleName + ":")
            SignatureReader(cn.signature)
//            .accept(trace)
                .accept(sp)
//                .accept(LoggingSignatureVisitor(Opcodes.ASM9, sp))

            println(trace.declaration)
        }
        val sp = SignatureParser(Opcodes.ASM9)
        val trace = TraceSignatureVisitor(0)

        classNode<GenericFields>().fields!!.forEach { fn ->

            val sp2 = SignatureParser(Opcodes.ASM9)
            println("${fn.name}:")
            SignatureReader(fn.signature)
                .accept(LoggingSignatureVisitor(Opcodes.ASM9, sp2))

            println(trace.declaration)
        }

    }
}

private class ClassExtendingAny<T>(var t: T) {

    fun <A : Int, B : String> methodAIntBString(a: A, b: B) {

    }
}

private interface InterfaceA<T : Comparable<T>, S : Map<Int, T>> : Comparable<T>

private class ClassExtendingMap() : HashMap<String, Double>()
private class ClassExtendingList() : ArrayList<String>() {
    val strings: List<String> = listOf()
}
private class ClassExtendingListMap() : ArrayList<HashMap<String, Double>>()

private class ClassExtendingAnyAny<T, U : List<T>, S : Map<T, U>>(var t: T, u: U)
private class ClassExtendingAnyAny2<T, U : List<T>, S : Map<T, String>>

private class GenericFields {
    var listOfString = listOf<String>()
    var arrayofString = listOf(arrayOf("hi"))
    var listOfNumber: List<Number>  = listOf()
    var mapOfKeyToValue: Map<Key, Value> = mapOf()
}

private class Key
private class Value