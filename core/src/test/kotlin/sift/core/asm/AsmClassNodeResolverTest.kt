package sift.core.asm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import sift.core.junit.LogActiveTestExtension
import java.io.FileNotFoundException
import java.net.UnknownHostException

@ExtendWith(LogActiveTestExtension::class)
class AsmClassNodeResolverTest {

    @Test
    fun `resolve classes from maven artifact id`() {
        assertThat(resolveClassNodes("net.onedaybeard.sift:core:0.9.0"))
            .isNotEmpty
    }

    @Test
    fun `resolve classes from maven artifact id with jar specifier`() {
        assertThat(resolveClassNodes("net.onedaybeard.sift:core:jar:0.9.0"))
            .isNotEmpty
    }

    @Test
    fun `parse classes from URI`() {
        assertThat(resolveClassNodes("https://repo1.maven.org/maven2/net/onedaybeard/sift/core/0.9.0/core-0.9.0.jar"))
            .isNotEmpty
    }


    @Nested
    inner class NegativeTests {

        @Test
        fun `fail resolving classes from invalid maven artifact id`() {
            assertThrows<IllegalStateException> {
                resolveClassNodes("net.onedaybeard.sift:core:0.9.0-yolo")
            }
        }

        @Test
        fun `fail resolving classes from invalid URI`() {
            assertThrows<FileNotFoundException> {
                resolveClassNodes("https://repo1.maven.org/maven2/net/onedaybeard/sift/core/0.9.0-yolo/core-0.9.0-yolo.jar")
            }
        }

        @Test
        fun `fail resolving classes from unknown host`() {
            assertThrows<UnknownHostException> {
                resolveClassNodes("https://locahost:12123/maven2/net/onedaybeard/sift/core/0.9.0/core-0.9.0.jar")
            }
        }
    }
}