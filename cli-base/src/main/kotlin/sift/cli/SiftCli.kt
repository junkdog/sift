@file:Suppress("MemberVisibilityCanBePrivate")
@file:OptIn(ExperimentalTime::class)

package sift.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import sift.core.api.MeasurementScope
import sift.core.api.PipelineProcessor
import sift.core.api.PipelineResult
import sift.core.api.debugLog
import sift.core.asm.classNodes
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.core.tree.TreeDsl.Companion.treeOf
import sift.instrumenter.Gruvbox.aqua2
import sift.instrumenter.Gruvbox.blue2
import sift.instrumenter.Gruvbox.dark2
import sift.instrumenter.Gruvbox.dark4
import sift.instrumenter.Gruvbox.fg
import sift.instrumenter.Gruvbox.gray
import sift.instrumenter.Gruvbox.green2
import sift.instrumenter.Gruvbox.light0
import sift.instrumenter.Gruvbox.light0Hard
import sift.instrumenter.Gruvbox.light3
import sift.instrumenter.Gruvbox.orange1
import sift.instrumenter.Gruvbox.purple2
import sift.instrumenter.Gruvbox.red1
import sift.instrumenter.Gruvbox.red2
import sift.instrumenter.Gruvbox.yellow1
import sift.instrumenter.Gruvbox.yellow2
import sift.instrumenter.InstrumenterService
import sift.instrumenter.Style
import sift.instrumenter.spi.InstrumenterServiceProvider
import java.nio.file.Path
import java.util.ServiceLoader
import kotlin.io.path.exists
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.ExperimentalTime

object SiftCli : CliktCommand(
    name = "sift",
    help = """
        A tool to model and analyze the design of systems from bytecode.
       
        The PATHS argument can occur anywhere, and multiple times, in the argument list.
        Any argument which isn't matched as an option is treated as an element of PATHS.
    """.trimIndent()
) {
    init {
        context { helpFormatter = CliktHelpFormatter(
            showDefaultValues = true,
            maxWidth = 105)
        }
    }

    val paths: List<Path> by argument()
        .path(mustExist = true)
        .help("jar or directory with classes")
        .multiple(required = false)
        .transformAll { paths ->
            paths.map { p ->
                p.resolve("target/classes")
                    .takeIf(Path::exists)
                    ?: p
            }
        }

    val instrumenter by option("-i", "--instrumenter",
            metavar = "INSTRUMENTER",
            help = "the instrumenter pipeline performing the scan",
            completionCandidates = CompletionCandidates.Fixed(instermenterNames().toSet()))
        .convert { instrumenters()[it] ?: fail("'$it' is not a valid instrumenter") }

    val listEntityTypes: Boolean by option("-t", "--list-entity-types",
            help = "lists entity types defined by instrumenter.")
        .flag()

    val maxDepth: Int? by option("-L", "--max-depth",
        help = "Max display depth of the tree")
        .int()
        .restrictTo(min = 0)

    val filter: List<Regex> by option("-f", "--filter",
            metavar = "REGEX",
            help = "filters nodes by label. can occur multiple times.")
        .convert { Regex(it) }
        .multiple()

    val filterContext: List<Regex> by option("-F", "--filter-context",
            metavar = "REGEX",
            help = "filters nodes by label, while also including sibling nodes." +
                " can occur multiple times.")
        .convert { Regex(it) }
        .multiple()

    val exclude: List<Regex> by option("-e", "--exclude",
            metavar = "REGEX",
            help = "excludes nodes when label matches REGEX. can occur multiple times.")
        .convert { Regex(it) }
        .multiple()

    val excludeTypes: List<Entity.Type> by option("-E", "--exclude-type",
            metavar = "ENTITY-TYPE",
            help = "excludes entity types from tree. can occur multiple times.")
        .convert { Entity.Type(it) }
        .multiple()

    val listInstrumenters: Boolean by option("-l", "--list-instrumenters",
            help = "print all instrumenters detected on the current classpath")
        .flag()

    val debug: Boolean by option("--debug",
        help = "prints log/logCount statements from the executed pipeline")
    .flag()

    val profile: Boolean by option("--profile",
        help = "prints execution times and input/output counts for the executed pipeline")
    .flag()

    val treeRoot: Entity.Type? by option("-T", "--tree-root",
            metavar = "ENTITY-TYPE",
            help = "tree built around requested entity type")
        .convert { Entity.Type(it) }

    val ansi: AnsiLevel? by option("-a", "--ansi",
            help = "override automatically detected ANSI support")
        .enum<AnsiLevel>(key = { it.name.lowercase() })

    private val noAnsi = Terminal(AnsiLevel.NONE)

    override fun run() {
        debugLog = debug

        val terminal = Terminal(ansi)

        when {
            listInstrumenters -> {
                instrumenters()
                    .map { (_, v) -> fg(v.name) }
                    .joinToString(separator = "\n")
                    .let(terminal::println)
            }
            listEntityTypes && instrumenter != null -> {
                buildTree()
                    ?.let { (pr, _) -> terminal.println(toString(instrumenter!!, pr.entityService)) }
                    ?: terminal.println(toString(instrumenter!!))
            }
            instrumenter == null -> {
                terminal.println("${orange1("Error: ")} ${fg("Must specify an instrumenter")}")
                exitProcess(1)
            }
            paths.isEmpty() -> throw PrintMessage("PATHS was not specified")
            profile -> {
                val (pr, tree) = buildTree(treeRoot)!!
                stylize(tree)
                filterTree(tree)

                // print headers
                (fg + bold)("  exec   in     out")
                    .let(terminal::println)
                terminal.println(pr.measurements.toString(
                    format = { measurement ->
                        when (measurement.measurementScope) {
                            MeasurementScope.Instrumenter -> light0Hard
                            MeasurementScope.Class -> aqua2 + bold
                            MeasurementScope.Field -> blue2
                            MeasurementScope.Method -> green2 + bold
                            MeasurementScope.Parameter -> purple2
                            MeasurementScope.FromContext -> red2 // shouldn't happen
                        }(measurement.action)
                    },
                    prefix = { measurement ->
                        val ms = (measurement.execution + 500.microseconds).inWholeMilliseconds
                        val gradient = listOf(dark4, gray, light3, yellow1, yellow2, red1, red2)
                        val c = if (ms < 1) {
                            dark2
                        } else {
                            gradient[max(0, min(gradient.lastIndex, log(ms.toDouble(), 2.5).toInt()))]
                        }

                        "${c("%3d ms")} ${light0("%4d -> %4d")}  "
                            .format(measurement.execution.inWholeMilliseconds, measurement.input, ms)
                    }
                ))
            }
            else -> { // render tree
                val (_, tree) = buildTree(treeRoot)!!
                stylize(tree)
                filterTree(tree)

                terminal.println(tree.toString(EntityNode::toString))
            }
        }

    }

    fun filterTree(tree: Tree<EntityNode>) {
        filter.applyToTree(tree) { matched ->
            matched
                .flatMap(Tree<EntityNode>::children)
                .flatMap { it.walk().toList() }
        }

        filterContext.applyToTree(tree) { matched ->
            // filter including sibling nodes in matched set
            matched
                .flatMap { it.parent?.children() ?: listOf(it) }
                .flatMap { it.walk().toList() }
        }

        exclude.forEach { regex ->
            tree.walk()
                .filter { regex in noAnsi.render(it.label) }
                .toList()
                .forEach(Tree<EntityNode>::delete)
        }

        excludeTypes.forEach { exclude ->
            tree.walk()
                .filter { it.value is EntityNode.Entity }
                .filter { node -> (node.value as EntityNode.Entity).entity.type == exclude }
                .toList()
                .forEach(Tree<EntityNode>::delete)
        }

        maxDepth?.let { depth ->
            tree.walk()
                .filter { it.depth == depth }
                .toList()
                .forEach(Tree<EntityNode>::delete)
        }
    }

    fun stylize(tree: Tree<EntityNode>) {
        val theme = instrumenter!!.theme()
        backtrackStyling(tree, theme)

        tree.walk().forEach { node ->
            when (val value = node.value) {
                is EntityNode.Entity -> {
                    value.label = theme[value.entity.type]
                        ?.format(node, theme)
                        ?: defaultStyle.format(node, theme)
                }
                is EntityNode.Label ->
                    value.label = gray(value.label)
            }
        }
    }

    fun backtrackStyling(tree: Tree<EntityNode>, theme: Map<Entity.Type, Style>) {
        fun e(node: Tree<EntityNode>?): EntityNode.Entity? {
            return when (val v = node?.value) {
                is EntityNode.Entity -> v
                is EntityNode.Label -> null
                null -> null
            }
        }

        // depth is typically 2..3, but culprit should be the minimum
        // backtrack depth (yes, it does make some unfortunate assumptions
        // about the tree structures).
        val backtrackDepth = tree.walk()
            .filter { e(it)?.entity?.children("backtrack")?.isNotEmpty() ?: false }
            .minByOrNull(Tree<EntityNode>::depth)
            ?.depth
            ?: return

        // format backtracked in tree
        var backtrackColumn: Int = 0

        // stylize backtracked nodes (as some may be missing from main tree nodesc)
        val styledBacktracked = tree.walk()
            .filter { it.depth == backtrackDepth }
            .flatMap { node ->
                when (val value = node.value) {
                    is EntityNode.Entity -> {
                        val entity = value.entity
                        val backtrack = entity.children("backtrack")
                        backtrackColumn = max(backtrackColumn, noAnsi.render(entity.label).length)

                        // need to wrap backtracked entities in a tree to make styling work (...)
                        treeOf(backtrack)
                            .children()
                            .map { n -> e(n)!!.entity to (theme[e(n)!!.entity.type] ?: defaultStyle).format(n, theme) }
                            .toList()


                    }
                    else -> listOf()
                }
        }.toMap()

        tree.walk()
            .filter { it.depth == backtrackDepth }
            .forEach { node ->
                when (val value = node.value) {
                    is EntityNode.Entity -> {
                        val entity = value.entity
                        val pad = "".padEnd(backtrackColumn - noAnsi.render(entity.label).length)

                        // skip backtracked entity if it's also the parent
                        (entity.children("backtrack") - (e(node.parent)?.entity ?: entity))
                            .takeIf(List<Entity>::isNotEmpty)
                            ?.map { e -> styledBacktracked[e] ?: e.label }
                            ?.let { styled ->
                                val formatted = styled.joinToString("${dark2(",")} ") { it }
                                value.label += "$pad    ${gray("<<<")} ${dark2("[")}$formatted${dark2("]")}"
                            }
                    }

                    else -> Unit
                }
            }
    }

    private fun buildTree(forType: Entity.Type? = null): Pair<PipelineResult, Tree<EntityNode>>? {
        val instrumenter = this.instrumenter ?: return null
        if (paths.isEmpty()) return null

        val pr: PipelineResult = PipelineProcessor(paths.flatMap(::classNodes))
            .execute(instrumenter.pipeline(), profile)

        return pr to instrumenter.toTree(pr.entityService, forType)
    }

    fun toString(instrumenter: InstrumenterService): String {
        val types = stylizedEntityTypes(instrumenter)
        return "${fg("entity types of ")}${(fg + bold)(instrumenter.name)}\n" + types.values
            .joinToString(separator = "\n") { label -> "${fg("-")} $label" }
    }

    fun toString(instrumenter: InstrumenterService, es: EntityService): String {
        val types = stylizedEntityTypes(instrumenter)
        return "${fg("entity types of ")}${(fg + bold)(instrumenter.name)}\n" + types
            .map { (type, label) -> es[type].size.toString().padStart(3) to label }
            .joinToString(separator = "\n") { (count, id) -> "${(fg + bold)(count)} $id" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun stylizedEntityTypes(instrumenter: InstrumenterService): Map<Entity.Type, String> {
        return tree("") {
            instrumenter
                .entityTypes
                .sortedBy(Entity.Type::id)
                .map { Entity(it, it.id) }
                .forEach(::add)
        }.also(::stylize)
            .children()
            .map { it as Tree<EntityNode.Entity> }
            .map { it.value.entity.type to it.value.label }
            .sortedBy { (type, _) -> type.id }
            .toMap()
    }

    fun Iterable<Regex>.applyToTree(
        tree: Tree<EntityNode>,
        f: (matched: List<Tree<EntityNode>>) -> Iterable<Tree<EntityNode>>
    ) {
        forEach { regex ->
            val (matched, discard) = tree.walk()
                .partition { regex in noAnsi.render(it.label) }
                .toList()

            val fullMatch = (matched +
                f(matched) +
                matched.flatMap(Tree<EntityNode>::parents)
            ).toSet()

            discard.filter { it !in fullMatch  }.forEach(Tree<EntityNode>::delete)
        }
    }
}

val defaultStyle = Style.Plain(fg)


fun instermenterNames() = instrumenters().map { (name, _)  -> name }

fun instrumenters(): Map<String, InstrumenterService> {
    return ServiceLoader
        .load(InstrumenterServiceProvider::class.java)
        .map(InstrumenterServiceProvider::create)
        .associateBy(InstrumenterService::name)
}

val Tree<EntityNode>.label: String
    get() = when (val v = value) {
        is EntityNode.Entity -> v.label
        is EntityNode.Label  -> v.label
    }

fun main(args: Array<String>) {
    SiftCli.completionOption().main(args)
}

