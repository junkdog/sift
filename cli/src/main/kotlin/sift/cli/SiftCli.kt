@file:Suppress("MemberVisibilityCanBePrivate")
@file:OptIn(ExperimentalTime::class)

package sift.cli

import com.fasterxml.jackson.module.kotlin.*
import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import com.github.ajalt.mordant.terminal.Terminal
import sift.core.api.*
import sift.core.asm.classNodes
import sift.core.entity.Entity
import sift.core.graphviz.DiagramGenerator
import sift.core.instrumenter.InstrumenterService
import sift.core.instrumenter.deserialize
import sift.core.jackson.*
import sift.core.tree.*
import sift.core.tree.DiffNode.State
import sift.core.tree.DiffNode.State.Unchanged
import sift.core.tree.TreeDsl.Companion.tree
import sift.core.tree.TreeDsl.Companion.treeOf
import sift.instrumenter.*
import sift.core.terminal.Gruvbox.aqua2
import sift.core.terminal.Gruvbox.blue1
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.dark2
import sift.core.terminal.Gruvbox.dark4
import sift.core.terminal.Gruvbox.fg
import sift.core.terminal.Gruvbox.gray
import sift.core.terminal.Gruvbox.green2
import sift.core.terminal.Gruvbox.light0
import sift.core.terminal.Gruvbox.light3
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.terminal.Gruvbox.purple2
import sift.core.terminal.Gruvbox.red1
import sift.core.terminal.Gruvbox.red2
import sift.core.terminal.Gruvbox.yellow1
import sift.core.terminal.Gruvbox.yellow2
import sift.core.terminal.Style
import sift.core.terminal.Style.Companion.diff
import sift.instrumenter.spi.InstrumenterServiceProvider
import java.io.File
import java.nio.file.Path
import java.util.Properties
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
    """.trimIndent()
) {
    init {
        context { helpFormatter = CliktHelpFormatter(
            showDefaultValues = true,
            maxWidth = 105)
        }
    }

    val path: Path? by option("-f", "--class-dir",
            metavar = "PATH",
            help = "Path to directory structure containing classes or path to .jar",
            completionCandidates = CompletionCandidates.Path
        )
        .path(mustExist = true)
        .help("jar or directory with classes")
        .convert { p -> p.resolve("target/classes").takeIf(Path::exists) ?: p }

    val listInstrumenters: Boolean by option("-l", "--list-instrumenters",
            help = "print all instrumenters detected on the current classpath")
        .flag()

    val instrumenter by option("-i", "--instrumenter",
            metavar = "INSTRUMENTER",
            help = "the instrumenter pipeline performing the scan",
            completionCandidates = CompletionCandidates.Fixed(instrumenterNames().toSet()))
        .convert { instrumenters()[it]?.invoke() ?: fail("'$it' is not a valid instrumenter") }

    val render: Boolean by option("-R", "--render",
        help = "render entities in graphviz's DOT language")
    .flag()

    val dumpSystemModel: Boolean by option("-X", "--dump-system-model",
        help = "print all entities along with their properties and metadata")
    .flag()

    val profile: Boolean by option("--profile",
        help = "print execution times and input/output for the executed pipeline")
    .flag()

    val treeRoot: Entity.Type? by option("-T", "--tree-root",
            metavar = "ENTITY-TYPE",
            help = "tree built around requested entity type")
        .convert { Entity.Type(it) }

    val listEntityTypes: Boolean by option("-t", "--list-entity-types",
            help = "lists entity types defined by instrumenter")
        .flag()

    val maxDepth: Int? by option("-L", "--max-depth",
        help = "Max display depth of the tree")
        .int()
        .restrictTo(min = 0)

    val filter: List<Regex> by option("-F", "--filter",
            metavar = "REGEX",
            help = "filters nodes by label. can occur multiple times")
        .convert { Regex(it) }
        .multiple()

    val filterContext: List<Regex> by option("-S", "--filter-context",
            metavar = "REGEX",
            help = "filters nodes by label, while also including sibling nodes." +
                " can occur multiple times")
        .convert { Regex(it) }
        .multiple()

    val exclude: List<Regex> by option("-e", "--exclude",
            metavar = "REGEX",
            help = "excludes nodes when label matches REGEX; can occur multiple times")
        .convert { Regex(it) }
        .multiple()

    val excludeTypes: List<Entity.Type> by option("-E", "--exclude-type",
            metavar = "ENTITY-TYPE",
            help = "excludes entity types from tree; can occur multiple times")
        .convert { Entity.Type(it) }
        .multiple()

    val save: File? by option("-s", "--save",
            metavar = "FILE_JSON",
            help = "save the resulting system model as json; for later use by --diff or --load",
            completionCandidates = CompletionCandidates.Path)
        .file(canBeDir = false)

    val load: File? by option("--load",
            metavar = "FILE_JSON",
            help = "load a previously saved system model",
            completionCandidates = CompletionCandidates.Path)
        .file(canBeDir = false, mustExist = true, mustBeReadable = true)

    val diff: File? by option("-d", "--diff",
            metavar = "FILE_JSON",
            help = "load a previously saved system model",
            completionCandidates = CompletionCandidates.Path)
        .file(canBeDir = false, mustExist = true, mustBeReadable = true)

    val ansi: AnsiLevel? by option("-a", "--ansi",
            help = "override automatically detected ANSI support")
        .enum<AnsiLevel>(key = { it.name.lowercase() })

    val version: Boolean by option("--version",
        help = "print version and release date")
    .flag()

    val debug: Boolean by option("--debug",
        help = "prints log/logCount statements from the executed pipeline")
    .flag()

    private val noAnsi = Terminal(AnsiLevel.NONE)

    override fun run() {
        debugLog = debug

        val terminal = Terminal(ansi)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val err = red2 + inverse + bold
            terminal.println("${(err)("${e::class.simpleName!!}:")} ${fg(e.message ?: "")}")
            terminal.println(fg("exiting..."))
        }

        when {
            version -> {
                val props = Properties()
                SiftCli::class.java.getResourceAsStream("/sift-metadata.properties")
                    .use(props::load)

                val version = props.getProperty("version")
                val timestamp = props.getProperty("timestamp")
                terminal.println("${light0("sift-$version")} (${fg(timestamp)})")
            }
            listInstrumenters -> {
                instrumenters()
                    .map { (_, v) -> fg(v().name) }
                    .joinToString(separator = "\n")
                    .let(terminal::println)
            }
            listEntityTypes && instrumenter != null -> {
                buildTree().let { (pr, _) -> terminal.println(toString(instrumenter!!, pr)) }
            }
            instrumenter == null -> {
                terminal.println("${orange1("Error: ")} ${fg("Must specify an instrumenter")}")
                exitProcess(1)
            }
            path == null && load == null -> throw PrintMessage("PATH was not specified")
            render -> {
                require(diff == null)
                val sm = systemModel()

                fun color(style: Style): String {
                    return when (style) {
                        is Style.Plain         -> style.styling.color?.toSRGB()?.toHex() ?: "#ffffff"
                        is Style.FromEntityRef -> color(style.fallback)
                        else                   -> "#ffffff"
                    }
                }

                val theme = instrumenter!!.theme()
                val lookup = theme
                    .map { (type, style) -> type to color(style) }
                    .toMap()
                    .let { lookup -> { type: Entity.Type -> lookup.getOrDefault(type, "#ffffff") } }

                // updating labels and filtering
                val tree = buildTree(sm, treeRoot)
                stylize(tree, theme)
                filterTree(tree)

                sm.entitiesByType.values.flatten().forEach { it.label = noAnsi.render(it.label) }

                val graph = DiagramGenerator(sm, lookup)
                val dot = graph.build(tree)
                noAnsi.println(dot)
            }
            dumpSystemModel -> dumpEntities(terminal)
            profile -> profile(terminal)
            diff != null -> {
                val tree = diffHead(loadSystemModel(diff!!), treeRoot, instrumenter!!)
                terminal.printTree(tree)
            }
            load != null -> {
                val sm = loadSystemModel(load!!)
                val tree = instrumenter!!.toTree(sm, treeRoot)

                terminal.printTree(tree)
            }
            else -> { // render tree from classes under path
                val (sm, tree) = buildTree(treeRoot)
                save?.let { out -> saveSystemModel(sm, out) }

                terminal.printTree(tree)
            }
        }

    }

    fun systemModel(): SystemModel {
        return if (load != null) {
            loadSystemModel(load!!)
        } else {
            PipelineProcessor(classNodes(path!!))
                .execute(instrumenter!!.pipeline(), profile)
        }
    }

    fun diffHead(
        deserializedResult: SystemModel,
        root: Entity.Type?,
        instrumenterService: InstrumenterService
    ): Tree<DiffNode> {
        val (_, new) = buildTree(root)
        val old = instrumenterService.toTree(deserializedResult, root)

        require(old.label == new.label)
        return Tree(DiffNode(Unchanged, new.value)).apply {
            merge(this, old.children(), new.children())
        }
    }

    private fun Terminal.printTree(
        tree: Tree<EntityNode>,
    ) {
        val theme = instrumenter!!.theme()
        stylize(tree, theme)
        filterTree(tree)
        backtrackStyling(tree, theme)

        println(tree.toString(EntityNode::toString))
    }

    @JvmName("printTreeDiff")
    private fun Terminal.printTree(
        diff: Tree<DiffNode>,
    ) {
        val tree = diff.map { n ->
            when (val wrapped = n.wrapped) {
                is EntityNode.Label -> wrapped
                is EntityNode.Entity -> {
                    wrapped["@diff"] = n.state
                    wrapped
                }
            }
        }

        val theme = instrumenter!!.theme()
            .map { (type, style) -> type to diff(style) }
            .toMap()

        stylize(tree, theme)
        filterTree(tree)
        backtrackStyling(tree, theme)

        println(tree.toString(
            prefix = { node ->
                when (node["@diff"] as State?) {
                    State.Added   -> " ${(aqua2 + bold)("+++")} "
                    State.Removed -> " ${(red2 + bold)("---")} "
                    else          -> "     "
                }
            },
            format = EntityNode::toString)
        )
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

    fun stylize(tree: Tree<EntityNode>, theme: Map<Entity.Type, Style>) {
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

        // depth is typically 2..4, but culprit should be the minimum
        // backtrack depth (yes, it does make some unfortunate assumptions
        // about the tree structures).
        val backtrackDepth = tree.walk()
            .filter { e(it)?.entity?.children("backtrack")?.isNotEmpty() ?: false }
            .minByOrNull(Tree<EntityNode>::depth)
            ?.depth
            ?: return

        // align backtracked in tree
        var backtrackColumn: Int = 0

        // stylize backtracked nodes (as some may be missing from main tree)
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

    private fun buildTree(forType: Entity.Type? = null): Pair<SystemModel, Tree<EntityNode>> {
        val instrumenter = this.instrumenter!!

        val sm: SystemModel = PipelineProcessor(classNodes(path!!))
            .execute(instrumenter.pipeline(), profile)

        return sm to instrumenter.toTree(sm, forType)
    }

    private fun buildTree(sm: SystemModel, forType: Entity.Type? = null): Tree<EntityNode> {
        return instrumenter!!.toTree(sm, forType)
    }

    fun toString(instrumenter: InstrumenterService): String {
        val types = stylizedEntityTypes(instrumenter)
        return "${fg("entity types of ")}${(fg + bold)(instrumenter.name)}\n" + types.values
            .joinToString(separator = "\n") { label -> "${fg("-")} $label" }
    }

    fun toString(instrumenter: InstrumenterService, pr: SystemModel): String {
        val types = stylizedEntityTypes(instrumenter)
        return "${fg("entity types of ")}${(fg + bold)(instrumenter.name)}\n" + types
            .map { (type, label) -> pr[type].size.toString().padStart(3) to label }
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
        }.also { stylize(it, instrumenter.theme()) }
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

    private fun dumpEntities(terminal: Terminal) {
        val sm = systemModel()

        fun entry(
            node: Tree<String>,
            padLength: Int,
            key: String,
            value: Any,
            style: TextStyle = aqua2
        ) {
            node.add(fg("$key:".padEnd(padLength)) + style(value.toString()))
        }

        val t = Tree(fg("entities"))
        fun addEntity(e: Entity) {
            t.add(Tree(fg("Entity[${green2(e.label.take(80))}]")).apply {
                entry(this, 20, "id", e.id)
                entry(this, 20, "type", e.type, orange1)
                add("children").let { children ->
                    e.children().forEach { child ->
                        entry(children, 17, child, e.children(child).joinToString { it.id.toString() })
                    }
                }
                add("properties").let { props ->
                    e.properties().forEach { (prop, value) ->
                        entry(props, 17, prop, value.joinToString(), green2)
                    }
                }
            })

        }

        sm.entitiesByType.values
            .flatten()
            .sortedBy { it.type.id }
            .forEach(::addEntity)

        terminal.println(t.toString(String::toString))
    }

    private fun profile(terminal: Terminal) {
        val (sm, _) = buildTree(treeRoot)
        fun MeasurementScope.style(): TextStyle = when (this) {
            MeasurementScope.Instrumenter -> fg
            MeasurementScope.Class        -> aqua2
            MeasurementScope.Field        -> blue2
            MeasurementScope.Method       -> green2
            MeasurementScope.Parameter    -> purple2
            MeasurementScope.FromContext  -> red2 // shouldn't happen often
            MeasurementScope.Signature    -> orange2
            MeasurementScope.TypeErased   -> blue1 + bold
        }

        val gradient = listOf(dark4, gray, light3, yellow1, yellow2, red1, red2)

        // print headers
        terminal.println((fg + bold)("  exec   in     out"))
        terminal.println(sm.measurements.toString(
            format = { measurement ->
                measurement.scopeIn.style()(measurement.action)
            },
            prefix = { measurement ->
                val ms = (measurement.execution + 500.microseconds).inWholeMilliseconds
                val c = if (ms < 1) {
                    dark2
                } else {
                    gradient[max(0, min(gradient.lastIndex, log(ms.toDouble(), 2.5).toInt()))]
                }

                val input = measurement.scopeIn.style()
                val output = when (measurement.scopeOut) {
                    MeasurementScope.FromContext -> measurement.scopeIn
                    else -> measurement.scopeOut
                }.style()

                if (measurement.execution.isPositive()) {
                    "${c("%3d ms")} ${input("%4d")} ${light0("->")} ${output("%4d")}  "
                        .format(ms, measurement.input, measurement.output)
                } else {
                    "${c("      ")} ${input("%4d")} ${light0("->")} ${output("%4d")}  "
                        .format(measurement.input, measurement.output)
                }
            }
        ))
    }
}

fun <T, R> Iterable<T>.pFlatMap(transform: (T) -> Iterable<R>): List<R> {
    return toList()
        .parallelStream()
        .flatMap { transform(it).toList().stream() }
        .toList()
}

fun instrumenterNames() = instrumenters().map { (name, _)  -> name }

fun instrumenters(): Map<String, () -> InstrumenterService> {
    val fromSpi = ServiceLoader
        .load(InstrumenterServiceProvider::class.java)
        .map(InstrumenterServiceProvider::create)
        .map { it.name to { it } }
        .toMap()

    // FIXME: windows paths
    val fromUserLocal = File("${System.getProperty("user.home")}/.local/share/sift/instrumenters")
        .also(File::mkdirs)
        .listFiles()!!
        .filter { it.extension == "json" }
        .map { file -> file.nameWithoutExtension to { InstrumenterService.deserialize(file.readText()) } }

    return (fromSpi + fromUserLocal).toSortedMap()
}

val defaultStyle = Style.Plain(fg)

fun main(args: Array<String>) {
    SiftCli.completionOption().main(args)
}
