package sift.core.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import sift.core.asm.classNode
import sift.core.dsl.Type

class MetadataParserTest {

    @Test
    fun `happy path - functions`() {

        val exhibitA = KotlinClass.from(classNode<ExhibitA>())!!

        assertThat(exhibitA.functions.values.map(KotlinFunction::toString))
            .containsExactlyInAnyOrder(
                "foo(string, int)",
                "infix foo(rhs)",
                "operator plus(rhs)",
                "inline operator String.minus(rhs)",
                "operator List<ExhibitA>.plus(rhs)",

                "Function0<Unit>.lambdaExtension()"
            )
    }

    @Test
    fun `recursive generic signature`() {
        val dg = KotlinClass.from(classNode<DeepGenerics>())!!

        val list = "kotlin.collections.List"
        val set = "kotlin.collections.Set"
        val map = "kotlin.collections.Map"

        assertThat(dg.functions.values.first().receiver)
            .isEqualTo(Type.from("$list<$set<$map<kotlin.String, kotlin.Int>>>"))

    }
}


@Suppress("unused")
private class ExhibitA {
    val propA: Char = 'a'
    val propB: Char
        get() = 'B'

    fun foo(string: String, int: Int = 2) = Unit
    infix fun foo(rhs: ExhibitA): ExhibitA = TODO()

    inline operator fun String.minus(rhs: ExhibitA): String = TODO()
    operator fun plus(rhs: ExhibitA): ExhibitA = TODO()

    fun (() -> Unit).lambdaExtension() = Unit

    operator fun List<ExhibitA>.plus(rhs: ExhibitA): List<ExhibitA> = TODO()

    companion object {
        fun bar(float: Float, byteArray: ByteArray) = Unit
    }
}

private class DeepGenerics {
    fun List<Set<Map<String, Int>>>.yolo() = Unit
}