package sift.core.asm.signature

import net.onedaybeard.collectionsby.firstBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer.MethodName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
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
            val signature = classNode(cls)
                .signature(LoggingSignatureVisitor())!!

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

    @Nested
    inner class MethodSignature {

        @Test
        fun `return list of string`() {
            assertSignature(
                cls = GenericMethods::class,
                name = "returnListOfString",
                typeParameters = null,
                methodParameters = null,
                returnType = "List<String>"
            )
        }

        @Test
        fun `return list of string array`() {
            assertSignature(
                cls = GenericMethods::class,
                name = "returnArrayofString",
                typeParameters = null,
                methodParameters = null,
                returnType = "List<String[]>"
            )
        }

        @Test
        fun `generic method parameters`() {
            assertSignature(
                cls = GenericMethods::class,
                name = "paramListOfNumber",
                typeParameters = "<T : Number>",
                methodParameters = "int, List<T>",
                returnType = "T"
            )
        }

        @Test
        fun `generic variable parameters`() {
            assertSignature(
                cls = GenericMethods::class,
                name = "paramMapOfKeyToValue",
                typeParameters = "<K : CharSequence, V>",
                methodParameters = "Map<K, V>",
                returnType = "void"
            )
        }

        private fun assertSignature(
            cls: KClass<*>,
            name: String,
            typeParameters: String?,
            methodParameters: String?,
            returnType: String?,
        ) {
            val cn = classNode(cls)
            val types = cn.signature()?.formalParameters ?: listOf()
            val signature = cn.methods
                .firstBy(MethodNode::name, name)
                .signature(types, LoggingSignatureVisitor())!!

            typeParameters?.let { expected ->
                assertThat("<${signature.formalParameters.joinToString()}>")
                    .isEqualTo(expected)
            } ?: run { assertThat(signature.formalParameters).isEmpty() }

            methodParameters?.let { expected ->
                assertThat(signature.methodParameters.joinToString())
                    .isEqualTo(expected)
            } ?: run { assertThat(signature.methodParameters).isEmpty() }

            assertThat(signature.returnType.toString())
                .isEqualTo(returnType)
        }
    }

    @Nested
    inner class FieldSignature {

        @Test
        fun `list of string`() {
            assertSignature(
                cls = GenericFields::class,
                name = "listOfString",
                typeParameters = null,
                extends = "List<String>"
            )
        }

        @Test
        fun `array of string`() {
            assertSignature(
                cls = GenericFields::class,
                name = "arrayofString",
                typeParameters = null,
                extends = "List<String[]>"
            )
        }

        @Test
        fun `list of number`() {
            assertSignature(
                cls = GenericFields::class,
                name = "listOfNumber",
                typeParameters = null,
                extends = "List<Number>"
            )
        }

        @Test
        fun `map of key value`() {
            assertSignature(
                cls = GenericFields::class,
                name = "mapOfKeyToValue",
                typeParameters = null,
                extends = "Map<Key, Value>"
            )
        }

        @Test
        fun `resolving formal type from class signature`() {
            assertSignature(
                cls = GenericFields2::class,
                name = "listOfT",
                typeParameters = "<T : [Number, Comparator<Key>]>",
                extends = "List<T>"
            )
        }

        private fun assertSignature(
            cls: KClass<*>,
            name: String,
            typeParameters: String?,
            extends: String
        ) {
            val cn = classNode(cls)
            val types = cn.signature()?.formalParameters ?: listOf()
            val signature = cn.fields
                .firstBy(FieldNode::name, name)
                .signature(types, LoggingSignatureVisitor())!!

            typeParameters?.let { expected ->
                assertThat("<${signature.formalParameters.joinToString()}>")
                    .isEqualTo(expected)
            } ?: run { assertThat(signature.formalParameters).isEmpty() }

            assertThat(signature.extends.toString())
                .isEqualTo(extends)
        }
    }
}

private interface InterfaceA<T : Comparable<T>, S : Map<Int, T>> : Comparable<T>
private class ClassExtendingMap() : HashMap<String, Double>()
abstract class ClassImplementingMapOfS<T, S : List<T>> : Map<String, S>


private class GenericFields {
    var listOfString = listOf<String>()
    var arrayofString = listOf(arrayOf("hi"))
    var listOfNumber: List<Number>  = listOf()
    var mapOfKeyToValue: Map<Key, Value> = mapOf()
}

private class GenericFields2<T> where T : Number, T : Comparator<Key> {
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