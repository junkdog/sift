package sift.core.terminal

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import sift.core.TemplateProcessingException
import sift.core.api.Measurement
import sift.core.api.MeasurementScope
import sift.core.terminal.Gruvbox.fg
import sift.core.terminal.Gruvbox.light0
import sift.core.terminal.Gruvbox.red2
import sift.core.tree.Tree

class ExceptionHandler(
    private val terminal: Terminal = Terminal(AnsiLevel.NONE),
    private val printStackTrace: Boolean = false
) {
    private val err = red2 + bold

    operator fun invoke(e: Throwable) {
        if (e is TemplateProcessingException) {
            if (e.trace != null) {
                printTrace(e)
                printException(e.cause)
            } else {
                printException(e.cause)
                terminal.println(fg("Error during template processing: Use ${(light0 + bold)("--profile")} " +
                    "option to identify the problematic operation."))
            }
        } else {
            printException(e)
        }
    }

    private fun printException(e: Throwable) {
        terminal.println(err("${e::class.simpleName}: ") + err(e.message ?: "Unknown error"))
        if (printStackTrace) {
            terminal.println(e.stacktrace)
            terminal.println()
        } else {
            terminal.println()
            terminal.println(fg("Run sift with the ${(light0 + bold)("--stacktrace")} option to print the full stack trace."))
        }
    }

    private fun printTrace(e: TemplateProcessingException) {
        // print exception message as header
        terminal.println(err("Error: ") + err(e.cause.message ?: "Unknown error"))
        terminal.println()

        // print processed actions; mark the problematic one
        e.trace as Tree<Measurement>
        e.trace.markError()
        printProfile(terminal, e.trace)
        terminal.println()
    }
}

private val Throwable.stacktrace: String
    get() = stackTraceToString()
        .lines()
        .drop(1)
        .joinToString("\n")
        .let { fg(it) }

private fun Tree<Measurement>.markError() {
    walk().last().value.apply {
        scopeIn = MeasurementScope.Exception
        scopeOut = MeasurementScope.Exception
    }
}
