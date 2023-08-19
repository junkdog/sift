package sift.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

class DebugOptions : OptionGroup(name = "Debug options") {
    val logs: Boolean by option("--debug",
        help = "Print log/logCount statements from the executed template.")
    .flag()

    val inverseTrace: Boolean by option("-X", "--debug-inverse-trace",
        help = "Print the inverse element trace for the elements specified with --debug-element-traces"
    ).flag()

    // TODO: also match by label
    val elementTraces: List<Int> by option("-x", "--debug-element-trace",
        help = "Print all element traces leading to the specified elements",
        metavar = "ELEMENT_ID",
    ).int().multiple()
}