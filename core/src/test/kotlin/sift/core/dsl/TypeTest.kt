package sift.core.dsl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import sift.core.api.*
import sift.core.api.testdata.set2.Repo
import sift.core.asm.classNode
import sift.core.element.AsmType
import sift.core.element.ClassNode
import kotlin.reflect.KClass


class TypeTest {

    init {
        debugLog = true
    }

    @Test
    fun `convert between asm type and sift type`() {
        val input = listOf(HashMap::class, Repo::class, TypeTest::class)
            .map(KClass<out Any>::java)
            .map(AsmType::getType)

        val expected = listOf(
            "java.util.HashMap".type,
            "sift.core.api.testdata.set2.Repo".type,
            "sift.core.dsl.TypeTest".type
        )

        assertThat(input.map { Type.from(it) })
            .containsExactlyElementsOf(expected)
    }

    @Test
    fun `type from asm ClassNode`() {
        assertThat(ClassNode.from(classNode(HashMap::class)).type)
            .isEqualTo("java.util.HashMap".type)
        assertThat(ClassNode.from(classNode(Repo::class)).type)
            .isEqualTo("sift.core.api.testdata.set2.Repo".type)
    }
}


