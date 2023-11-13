package sift.core.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sift.core.asm.classNode
import sift.core.element.ClassNode
import sift.core.junit.LogActiveTestExtension

@ExtendWith(LogActiveTestExtension::class)
internal class TraceTest {
    val elements = generateSequence { ClassNode.from(classNode(TraceTest::class)) }
        .onEachIndexed { index, elem -> elem.id = index }
        .take(10)
        .toList()

    fun trace(vararg ids: Int): ElementTrace {
        val ids = ids.reversed()
        return ids.drop(1).fold(ElementTrace(elements[ids[0]])) { acc, i ->  acc + elements[i] }
    }

    @Test
    fun `ElementTrace containing ElementTrace`() {
        val a = trace(   1,    5, 6)
        val b = trace(0, 1, 4, 5, 6, 7)

        assertThat(a in b).isTrue()
        assertThat(b in a).isFalse()

        assertThat(a !in b).isFalse()
        assertThat(b !in a).isTrue()
    }

    @Test
    fun `ElementTrace containing prospect ElementTrace`() {
        val a = trace(   3,    7)
        val b = trace(0, 3, 4, 7, 9)

        assertThat((a to elements[9]) in b).isTrue()
        assertThat((a to elements[9]) !in b).isFalse()

        assertThat((a to elements[8]) in b).isFalse()
        assertThat((a to elements[8]) !in b).isTrue()
    }
}
