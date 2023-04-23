package sift.core.dsl

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sift.core.api.*
import sift.core.api.testdata.set2.Repo
import sift.core.api.testdata.set2.RepoT
import sift.core.asm.classNode
import sift.core.element.AsmType
import sift.core.element.ClassNode
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.template.SystemModelTemplate
import sift.core.template.load
import java.util.HashMap
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


