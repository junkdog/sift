package sift.core.terminal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import sift.core.terminal.TextTransformer.Companion.dedupe
import sift.core.terminal.TextTransformer.Companion.idSequence
import sift.core.terminal.TextTransformer.Companion.replace
import sift.core.terminal.TextTransformer.Companion.uuidSequence
import java.util.*

class TextTransformerTest {

    @Test
    fun `dedupe removes duplicate characters`() {
        val transformer = dedupe('a')
        assertThat(transformer("a")).isEqualTo("a")
        assertThat(transformer("aaa")).isEqualTo("a")
        assertThat(transformer("aab")).isEqualTo("ab")
    }

    @Test
    fun `idSequence replaces regex matches with sequential values`() {
        idSequence(Regex("[0-9]+")).let { transformer ->
            assertThat(transformer("123 hej")).isEqualTo("1 hej")
        }

        val uuid = UUID::randomUUID

        uuidSequence().let { transformer ->
            assertThat(transformer("foo ${uuid()} ${uuid()} 1234")).isEqualTo("foo 1 2 1234")
        }
    }

    @Test
    fun `replace replaces all occurrences of a string with a given value`() {
        val transformer = replace("abc", "def")
        assertThat(transformer("abc")).isEqualTo("def")
        assertThat(transformer("abcxabc")).isEqualTo("defxdef")
    }

    @Test
    fun `replace replaces all occurrences of a regex with a given value`() {
        val transformer = replace(Regex("[0-9]{1,3}"), "x")
        assertThat(transformer("123")).isEqualTo("x")
        assertThat(transformer("1234")).isEqualTo("xx")
    }
}
