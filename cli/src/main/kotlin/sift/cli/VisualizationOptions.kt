package sift.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import sift.core.graphviz.EdgeLayout

class VisualizationOptions : OptionGroup(name = "Visualization options") {
    val render: Boolean by option("-R", "--render",
        help = "Render entities with graphviz's DOT language.")
    .flag()

    val edgeLayout: EdgeLayout by option("--edge-layout",
            help = "Sets the layout for the  lines between nodes.")
        .enum<EdgeLayout>()
        .default(EdgeLayout.spline)
}