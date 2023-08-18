@file:OptIn(ExperimentalTime::class)

package sift.template

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import sift.cli.formatted
import sift.core.api.Action
import sift.core.api.SystemModel
import sift.core.api.TemplateProcessor
import sift.core.entity.Entity
import sift.core.template.toTree
import sift.core.terminal.generateProfilerView
import java.net.URL
import kotlin.time.ExperimentalTime

abstract class TemplateValidator(
    template: Action<Unit, Unit>,
    jar: URL,
    val root: Entity.Type,
    val expectedTree: String,
    val expectedProfile: String,
    val expectedStatistics: String,
) {
    private val sm = TemplateProcessor.from(jar.toString(), listOf())
        .execute(template, true)

    @Test
    fun tree()       = sm.validateTree(root, expectedTree)
    @Test
    fun profile()    = sm.validateProfile(expectedProfile)
    @Test
    fun statistics() = sm.validateStatistics(expectedStatistics)
}

private fun SystemModel.validateTree(
    root: Entity.Type,
    expected: String
) {
    val actual = toTree(listOf(root))
        .let(terminal::render)
        .lines().dropLast(1).joinToString("\n")

    assertThat(actual).isEqualTo(expected.trimIndent())
}

private fun SystemModel.validateProfile(
    expected: String
) {
    val actual = generateProfilerView(terminal, measurements)
        .lines()
        .dropLast(1)
        .map(String::zeroedMeasurements)
        .joinToString("\n")

    assertThat(actual)
        .isEqualTo(expected
            .lines()
            .drop(1)
            .map(String::zeroedMeasurements)
            .joinToString("\n"))
}

private val String.zeroedMeasurements: String get() = replace(matchTiming, "  0.00 ms")

private fun SystemModel.validateStatistics(
    statistics: String,
) {
    assertThat(statistics().formatted.stripTimingMeasurements)
        .isEqualTo(statistics.trimIndent().stripTimingMeasurements)
}

private val String.stripTimingMeasurements: String
    get() = lines().filter { "timing.ms" !in it }.joinToString("\n")

private val matchTiming = Regex("^\\s*\\d+\\.\\d{2} ms")

private val terminal = Terminal(AnsiLevel.NONE)
