package sift.core.asm.signature

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes.ASM9
import sift.core.asm.*
import kotlin.reflect.KClass

class GenericsTest {

    @Nested
    inner class ClassSignature {
        @Test
        fun `nested generics`() {
            assertSignature(
                cls = InterfaceA::class,
                typeParameters = "<T : Comparable<T>, S : Map<Integer, T>>",
                extends = "Object",
                implements = "[Comparable<T>]"
            )
        }

        @Test
        fun `extending map of concrete types`() {
            assertSignature(
                cls = ClassExtendingMap::class,
                typeParameters = null,
                extends = "HashMap<String, Double>",
                implements = null,
            )
        }

        @Test
        fun `implementing map of concrete and variable type`() {
            assertSignature(
                cls = ClassImplementingMapOfS::class,
                typeParameters = "<T, S : List<T>>",
                extends = null,
                implements = "[Map<String, S>, KMappedMarker]", // TODO: consider stripping internal annotations
            )
        }

        private fun assertSignature(
            cls: KClass<*>,
            typeParameters: String?,
            extends: String?,
            implements: String?,
        ) {
            val signature = classNode(cls).signature(LoggingSignatureVisitor())!!

            typeParameters?.let { expected ->
                assertThat("<${signature.formalParameters.joinToString()}>")
                    .isEqualTo(expected)
            } ?: run { assertThat(signature.formalParameters).isEmpty() }

            extends?.let { expected ->
                assertThat(signature.extends.toString())
                    .isEqualTo(expected)
            } ?: run { assertThat(signature.extends.toString()).isEqualTo("Object") }

            implements?.let { expected ->
                assertThat(signature.implements.toString())
                    .isEqualTo(expected)
            } ?: run { assertThat(signature.implements).isEmpty() }
        }
    }

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
            println(cn.name)
            println(cn.signature(LoggingSignatureVisitor(ASM9)))
            println()
        }


        classNode<GenericFields>().fields!!.forEach { fn ->
            println(fn.name)
            println(fn.signature(listOf(), LoggingSignatureVisitor(ASM9)))
            println()
        }


        val genericFields2 = classNode(GenericFields2::class)
        val formalTypeParams = genericFields2.signature()!!.formalParameters
        genericFields2.fields!!.forEach { fn ->
            println(fn.name)
            println(fn.signature(formalTypeParams, LoggingSignatureVisitor(ASM9)))
            println()
        }

        classNode<GenericMethods>().methods!!.filter { it.signature != null }.forEach { mn ->
            println(mn.name)
            println(mn.signature(listOf(), LoggingSignatureVisitor(ASM9)))
            println()
        }
    }
}

private interface InterfaceA<T : Comparable<T>, S : Map<Int, T>> : Comparable<T>
private class ClassExtendingMap() : HashMap<String, Double>()
abstract class ClassImplementingMapOfS<T, S : List<T>> : Map<String, S>



private class ClassExtendingAny<T>(var t: T)


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