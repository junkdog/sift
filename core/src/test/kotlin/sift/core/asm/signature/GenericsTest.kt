package sift.core.asm.signature

import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.util.TraceSignatureVisitor
import sift.core.asm.*

class GenericsTest {
    @Test
    fun `read simple class signature`() {
        listOf(
            classNode(ClassExtendingMap::class),
            classNode(ClassExtendingList::class),
            classNode(ClassExtendingListMap::class),
            classNode(ClassExtendingAnyAny::class),
            classNode(ClassExtendingAnyAny2::class),
            classNode(InterfaceA::class),
        ).forEach { cn ->

            val sp = SignatureParser(Opcodes.ASM9, LoggingSignatureVisitor(Opcodes.ASM9))

            println(cn.type.simpleName + ":")
            SignatureReader(cn.signature)
                .accept(sp)

            println()
            println(sp.asClassSignatureNode)
            println()
        }


        classNode<GenericFields>().fields!!.forEach { fn ->
            SignatureParser(ASM9, LoggingSignatureVisitor(ASM9)).let { sp ->
                println("${fn.name}:")
                SignatureReader(fn.signature)
                    .accept(sp)

                println()
                println(sp.asTypeSignatureNode)
                println()
            }
        }


        val genericFields2 = classNode(GenericFields2::class)

        val formalTypeParams = SignatureParser(ASM9, null)
            .also { SignatureReader(genericFields2.signature).accept(it) }
            .typeParameters

        SignatureParser(formalTypeParams, ASM9, LoggingSignatureVisitor(ASM9)).let { sp ->
            genericFields2.fields!!.forEach { fn ->

                println("${fn.name}:")
                SignatureReader(fn.signature)
                    .accept(sp)

                println()
                println(sp.asTypeSignatureNode)
                println()
            }
        }

        SignatureParser(ASM9, LoggingSignatureVisitor(ASM9)).let { sp ->
            classNode<GenericMethods>().methods!!.filter { it.signature != null }.forEach { mn ->

                println("${mn.name}:")
                SignatureReader(mn.signature)
                    .accept(sp)


                println()
                println(sp.asMethodSignatureNode)
                println()
            }
        }
    }
}

private class ClassExtendingAny<T>(var t: T)

private interface InterfaceA<T : Comparable<T>, S : Map<Int, T>> : Comparable<T>

private class ClassExtendingMap() : HashMap<String, Double>()
private class ClassExtendingList() : ArrayList<String>() {
    val strings: List<String> = listOf()
}
private class ClassExtendingListMap() : ArrayList<HashMap<String, Double>>()

private class ClassExtendingAnyAny<T, U : List<T>, S : Map<T, U>>(var t: T, u: U)
abstract class ClassExtendingAnyAny2<T, U : List<T>, S : Map<T, String>> : Map<T, String>

private class GenericFields {
    var listOfString = listOf<String>()
    var arrayofString = listOf(arrayOf("hi"))
    var listOfNumber: List<Number>  = listOf()
    var mapOfKeyToValue: Map<Key, Value> = mapOf()
}

private class GenericFields2<T : Number> {
    var listOfT = listOf<T>()
}

private class GenericMethods {
    fun returnListOfString(): List<String> = TODO()
    fun returnArrayofString(): List<Array<String>> = TODO()
    fun <T : Number> paramListOfNumber(yolo: Int, p: List<T>): T = TODO()
    fun <K : CharSequence, V> paramMapOfKeyToValue(m : Map<K, V>) = Unit
}

private class Key
private class Value