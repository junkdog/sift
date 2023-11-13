package sift.core

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sift.core.api.debug.intoTree
import sift.core.junit.LogActiveTestExtension
import sift.core.terminal.stripEmoji
import sift.core.tree.merge

@ExtendWith(LogActiveTestExtension::class)
class FnsTest {
    @Test
    fun `no emoji - happy path`() {
        val input = "   │  └─ \uD83C\uDF4E\uD83C\uDF4E :/no-emoji\uD83C\uDF4E"

        assertThat(input.stripEmoji())
            .isEqualTo("   │  └─ :/no-emoji")
    }

    @Test
    fun `merge two trees`() {
        val terminal = Terminal(AnsiLevel.NONE)

        val a = listOf("a", "b", "c", "d")
        val b = listOf("a", "b", "c", "e")

        fun validateTree(
            l: List<String>,
            r: List<String>,
            expected: String,
        ) {
            val actual = merge("root", l.intoTree(), r.intoTree()) { n, _ -> n }
                .let(terminal::render)
                .lines()
                .dropLast(1)
                .joinToString("\n")

            assertThat(actual).isEqualTo(expected.trimIndent())
        }

        validateTree(a, b, """
            ── root
               └─ a
                  └─ b
                     └─ c
                        ├─ d
                        └─ e
        """)

        validateTree(a.reversed(), b.reversed(), """
            ── root
               ├─ d
               │  └─ c
               │     └─ b
               │        └─ a
               └─ e
                  └─ c
                     └─ b
                        └─ a
        """)
    }

    @Test
    fun `merge two trees and map to node type to integer`() {
        val terminal = Terminal(AnsiLevel.NONE)

        val a = listOf("a", "b", "c", "d")
        val b = listOf("a", "b", "c", "e")

        fun validateTree(
            l: List<String>,
            r: List<String>,
            expected: String,
        ) {
            val actual = merge(0, l.intoTree(), r.intoTree()) { n, _ -> n[0].code }
                .let(terminal::render)
                .lines()
                .dropLast(1)
                .joinToString("\n")

            assertThat(actual).isEqualTo(expected.trimIndent())
        }

        validateTree(a, b, """
            ── 0
               └─ 97
                  └─ 98
                     └─ 99
                        ├─ 100
                        └─ 101
        """)
    }
}