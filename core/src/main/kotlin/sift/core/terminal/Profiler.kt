package sift.core.terminal

import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.italic
import com.github.ajalt.mordant.terminal.Terminal
import sift.core.api.Measurement
import sift.core.api.MeasurementScope
import sift.core.terminal.Gruvbox.aqua1
import sift.core.terminal.Gruvbox.aqua2
import sift.core.terminal.Gruvbox.blue1
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.dark3
import sift.core.terminal.Gruvbox.dark4
import sift.core.terminal.Gruvbox.fg
import sift.core.terminal.Gruvbox.gray
import sift.core.terminal.Gruvbox.green2
import sift.core.terminal.Gruvbox.light0
import sift.core.terminal.Gruvbox.light3
import sift.core.terminal.Gruvbox.orange2
import sift.core.terminal.Gruvbox.purple2
import sift.core.terminal.Gruvbox.red1
import sift.core.terminal.Gruvbox.red2
import sift.core.terminal.Gruvbox.yellow1
import sift.core.terminal.Gruvbox.yellow2
import sift.core.tree.Tree
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min


fun printProfile(
    terminal: Terminal,
    trace: Tree<Measurement>
) {
    // print headers
    terminal.println((fg + bold)("     exec  ety#    in      out"))

    var lastEntityCount = 0
    terminal.println(trace.toString(
        format = { measurement ->
            measurement.scopeIn.style()(measurement.action.replace(Label.match, Label.style("\$1")))
        },
        prefix = { measurement ->
            val ms = measurement.execution.inWholeMicroseconds / 1000.0
            val c = if (ms < 1) {
                dark3
            } else {
                gradient[max(0, min(gradient.lastIndex, log(ms, 2.5).toInt()))]
            }

            val c2 = if (measurement.entites > lastEntityCount) (fg + bold) else dark4
            lastEntityCount = measurement.entites

            val input = measurement.scopeIn.style()
            val output = when (measurement.scopeOut) {
                MeasurementScope.FromContext -> measurement.scopeIn
                else                         -> measurement.scopeOut
            }.style()

            if (measurement.execution.isPositive()) {
                "${c("%6.2f ms")} ${c2("%5d")} ${input("%5d")} ${light0("->")} ${output("%5d")}  "
                    .format(ms, measurement.entites, measurement.input, measurement.output)
            } else {
                "${c("         ")} ${c2("%5s")} ${input("%5d")} ${light0("->")} ${output("%5d")}  "
                    .format("", measurement.input, measurement.output)
            }
        }
    ))
}

private object Label {
    val match = Regex("(\"[^\"]*\")")
    val style = dark4 + italic + bold
}

private val gradient = listOf(
    dark4,
    gray,
    light3,
    yellow1,
    yellow2,
    red1,
    red2
)

private fun MeasurementScope.style(): TextStyle = when (this) {
    MeasurementScope.Template    -> fg
    MeasurementScope.Annotation  -> aqua1 + bold
    MeasurementScope.Class       -> aqua2
    MeasurementScope.Field       -> blue2
    MeasurementScope.Method      -> green2
    MeasurementScope.Parameter   -> purple2
    MeasurementScope.FromContext -> red2 // shouldn't happen often
    MeasurementScope.Signature   -> orange2
    MeasurementScope.TypeErased  -> blue1 + bold

    MeasurementScope.Exception   -> red2 + bold
}