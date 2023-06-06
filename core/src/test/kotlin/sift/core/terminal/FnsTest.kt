package sift.core.terminal

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class FnsTest {
    @Test
    fun `no emoji - happy path`() {
        val input = "   │  └─ \uD83C\uDF4E\uD83C\uDF4E :/no-emoji\uD83C\uDF4E"

        Assertions.assertThat(input.stripEmoji())
            .isEqualTo("   │  └─ :/no-emoji")
    }
}