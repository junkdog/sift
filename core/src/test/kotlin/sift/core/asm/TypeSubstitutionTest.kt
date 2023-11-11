package sift.core.asm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.objectweb.asm.commons.SimpleRemapper
import sift.core.api.parseSignature
import sift.core.asm.signature.signature
import sift.core.junit.LogActiveTestExtension

@ExtendWith(LogActiveTestExtension::class)
class TypeSubstitutionTest {

    @Test
    fun `remap plain generic class`() {

        val cn = classNode(TestClasses.Foo::class)
        val refSignature = classNode(TestClasses.FooAbc::class)
            .signature()
        val updated = classNode(TestClasses.FooBar::class)
            .signature()

//        val (a, b) = listOf(refSignature, updated).map(::parseSignature)
        val remapped = cn.copy(SimpleRemapper(mapOf("GENERIC_T" to "sift.core.asm.TestClasses.Bar")))

        assertThat(remapped.toDebugString()).isEqualTo(cn.toDebugString())
    }
}

private object TestClasses {
    open class Foo<GENERIC_T> {
        fun method(t: GENERIC_T): GENERIC_T = t
    }

    class FooAbc<ABC> : Foo<ABC>()
    class FooBar : Foo<Bar>()

    class Bar

}

class EnhancedRemapper(mapping: Map<String, String>) : SimpleRemapper(mapping) {

}