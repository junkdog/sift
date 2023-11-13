package sift.core.junit

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import com.github.ajalt.mordant.terminal.Terminal
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import sift.core.terminal.Gruvbox.dark2
import sift.core.terminal.Gruvbox.dark4
import sift.core.terminal.Gruvbox.fg
import sift.core.terminal.Gruvbox.gray

class LogActiveTestExtension : BeforeEachCallback {

    val terminal = Terminal(AnsiLevel.TRUECOLOR)

    private val prefix = listOf(
        (fg + bold + inverse)(" "),
        (gray on fg + bold)("▶"),
        (dark4 on fg + bold)("▶"),
        (dark2 on fg + bold)("▶"),
        (fg + bold + inverse)(" "),
    ).joinToString("")

    val suffix = listOf(
        (gray + bold + inverse)(" "),
        (dark4 + bold + inverse)(" "),
        (dark2 + bold + inverse)(" "),
    ).joinToString("")

    override fun beforeEach(context: ExtensionContext) =
        "$prefix${(fg + bold + inverse)("${context.testName} ")}${suffix}"
            .let(terminal::println)

    private val ExtensionContext.testName
        get() = "${testClass.map(Class<*>::getSimpleName).get()}::${displayName}"
}