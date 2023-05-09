@file:Suppress("MemberVisibilityCanBePrivate")

package sift.cli

import com.fasterxml.jackson.module.kotlin.*
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import sift.core.api.*
import sift.core.asm.resolveClassNodes
import sift.core.entity.Entity
import sift.core.graphviz.DiagramGenerator
import sift.core.jackson.*
import sift.core.template.SystemModelTemplate
import sift.core.template.deserialize
import sift.core.terminal.Gruvbox.aqua2
import sift.core.terminal.Gruvbox.blue1
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.dark2
import sift.core.terminal.Gruvbox.dark3
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
import sift.core.terminal.TextTransformer.Companion.uuidSequence
import sift.core.tree.*
import sift.core.tree.DiffNode.State
import sift.core.tree.DiffNode.State.Unchanged
import sift.core.tree.TreeDsl.Companion.tree
import sift.core.tree.TreeDsl.Companion.treeOf
import sift.template.*
import sift.template.sift.SiftSelfTemplate
import sift.template.spi.SystemModelTemplateServiceProvider
import java.io.File
import java.net.URI
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime


object SiftCli : CliktCommand(
    name = "sift",
    help = """
        Sift is a command-line tool that analyzes and reverse models software system designs
        from JVM bytecode.
    """.trimIndent(),
    epilog = """
        ```
        ${(fg + bold)("Examples:")}
          ${fg("sift --template spring-axon -f my-spring-project")}
          Model the system using the "spring-axon" template on the classes in the  
          "my-spring-project" directory.
        
          ${fg("sift -t spring-axon -f . -F \"Order(Created|Shipped)\" --save feature-01.json")}
          Model the system using the "spring-axon" template on the current directory's
          classes, filter nodes containing the regular expression "Order(Created|Shipped)",
          and save the system model to "feature-01.json".
        
          ${fg("sift -t spring-axon -f . --diff feature-01.json")}
          Compare the current design of the system using the "spring-axon" template on
          the classes in the current directory against a previously saved system model
          from "feature-01.json" and show the differences.
          
          ${fg("sift -t spring-axon -f . -F \"Product\" --render")}
          Model the system using the "spring-axon" template on the current directory's 
          classes, filter the graph to show only nodes containing "Product", and render
          the result using graphviz's DOT language.
        ```
    """.trimIndent()
) {
    init {
        context {
            helpFormatter = object : CliktHelpFormatter(
                showDefaultValues = true,
                maxWidth = 90
            ) {
                override fun renderSectionTitle(title: String): String = when(title) {
                    "Options:" -> "Miscellaneous options:"
                    else -> title
                }.let((fg + bold)::invoke)
            }
        }
    }

    val template by TemplateOptions()
    val tree by EntityTreeOptions()
    val graphviz by VisualizationOptions()
    val serialization by SerializationOptions()

    val ansi: AnsiLevel? by option("-a", "--ansi",
        help = "Override automatically detected ANSI support."
    ).enum<AnsiLevel>(key = { it.name.lowercase() })

    val stacktrace: Boolean by option("--stacktrace",
        help = "Print stacktrace to stderr if an error occurs"
    ).flag(default = false)

    val version: Boolean by option("--version",
        help = "Print version and release date.")
    .flag()

    val debug: Boolean by option("--debug",
        help = "Print log/logCount statements from the executed template.")
    .flag()

    val statistics: Boolean by option("--statistics",
        help = "Print internal statistics about the system model template context."
    ).flag()

    private val noAnsi = Terminal(AnsiLevel.NONE)

    override fun run() {
        debugLog = debug

        val terminal = Terminal(ansi)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val err = red2 + inverse + bold
            terminal.forStdErr().apply {
                println("${err("${e::class.simpleName!!}:")} ${fg(e.message ?: "")}")
                if (stacktrace) {
                    val trace = e.stackTraceToString()
                        .lines()
                        .drop(1)
                        .joinToString("\n")
                    println(fg(trace))
                }

                println(fg("exiting..."))
            }
        }

        validateParameterOptions()

        when {
            version -> {
                val metadata = loadMetadata()

                val version = metadata("version")
                val timestamp = metadata("timestamp")
                terminal.println("${light0("sift-$version")} (${fg(timestamp)})")
            }
            template.listTemplates -> {
                templates()
                    .map { (_, v) -> fg(v().name.padEnd(25)) to v().description }
                    .joinToString(separator = "\n") { (name, desc) -> "$name ${desc.lines().first()}" }
                    .let(terminal::println)
            }
            template.listEntityTypes && template.template != null -> {
                when {
                    template.classNodes != null ->
                        buildTree(tree.treeRoot).let { (pr, _) -> terminal.println(toString(template.template!!, pr)) }

                    serialization.load != null ->
                        terminal.println(toString(template.template!!, loadSystemModel(serialization.load!!)))

                    else -> terminal.println(toString(template.template!!))
                }
            }
            statistics && template.template != null -> {
                val sm = systemModel()
                val format: (Any) -> String = numberFormatter()

                sm.statistics().entries
                    .joinToString("\n") { (k, v) -> "%-40s %8s".format(k, format(v)) }
                    .let(terminal::println)
            }
            template.template == null -> {
                terminal.println("${orange1("Error: ")} ${fg("Must specify a template, use -l to list available templates.)")}")
                exitProcess(1)
            }
            template.classNodes == null && serialization.load == null -> throw PrintMessage("PATH was not specified")
            graphviz.render -> {
                require(template.diff == null)
                val sm = systemModel()

                fun color(style: Style): String {
                    return when (style) {
                        is Style.Plain         -> style.styling.color?.toSRGB()?.toHex() ?: "#ffffff"
                        is Style.FromEntityRef -> color(style.fallback)
                        else                   -> "#ffffff"
                    }
                }

                val theme = template.template!!.theme()
                val lookup = theme
                    .map { (type, style) -> type to color(style) }
                    .toMap()
                    .let { lookup -> { type: Entity.Type -> lookup.getOrDefault(type, "#ffffff") } }

                // updating labels and filtering
                val tree = buildTree(sm, tree.treeRoot)
                stylize(tree, theme)
                filterTree(tree)

                sm.entitiesByType.values.flatten().forEach { it.label = noAnsi.render(it.label) }

                val graph = DiagramGenerator(sm, graphviz.edgeLayout, lookup)
                val dot = graph.build(tree)
                noAnsi.println(dot)
            }
            template.dumpSystemModel -> dumpEntities(terminal)
            template.profile -> profile(terminal)
            template.diff != null -> {
                val other = resolveSystemModel(template.diff!!, template.template, template.mavenRepositories)
                val tree = diffHead(other, this.tree.treeRoot, template.template!!)
                terminal.printTree(tree)
            }
            serialization.load != null -> {
                val sm = loadSystemModel(serialization.load!!)
                val tree = template.template!!.toTree(sm, this.tree.treeRoot)

                terminal.printTree(tree)
            }
            else -> { // render tree from classes under path
                val (sm, tree) = buildTree(this.tree.treeRoot)
                serialization.save?.let { out -> saveSystemModel(sm, out) }

                terminal.printTree(tree)
            }
        }

    }

    private fun validateParameterOptions() {
        if (serialization.save != null && listOfNotNull(template.diff, serialization.load).isNotEmpty())
            error("Cannot use --save with --load or --diff")
    }

    fun systemModel(): SystemModel {
        return if (serialization.load != null) {
            loadSystemModel(serialization.load!!)
        } else {
            resolveClassNodes(template.classNodes!!, template.mavenRepositories)
                .let(::TemplateProcessor)
                .execute(template.template!!.template(), template.profile)
        }
    }

    fun diffHead(
        deserializedResult: SystemModel,
        roots: List<Entity.Type>,
        template: SystemModelTemplate
    ): Tree<DiffNode> {
        val (_, new) = buildTree(roots)
        val old = template.toTree(deserializedResult, roots)

        require(old.label == new.label)
        return Tree(DiffNode(Unchanged, new.value)).apply {
            merge(this, old.children(), new.children())
        }
    }

    private fun Terminal.printTree(
        tree: Tree<EntityNode>,
    ) {
        val theme = template.template!!.theme()
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

        val theme = template.template!!.theme()
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
        this.tree.filter.applyToTree(tree) { matched ->
            matched
                .flatMap(Tree<EntityNode>::children)
                .flatMap { it.walk().toList() }
        }

        this.tree.filterContext.applyToTree(tree) { matched ->
            // filter including sibling nodes in matched set
            matched
                .flatMap { it.parent?.children() ?: listOf(it) }
                .flatMap { it.walk().toList() }
        }

        this.tree.exclude.forEach { regex ->
            tree.walk()
                .filter { regex in noAnsi.render(it.label) }
                .toList()
                .forEach(Tree<EntityNode>::delete)
        }

        this.tree.excludeTypes.forEach { exclude ->
            tree.walk()
                .filter { it.value is EntityNode.Entity }
                .filter { node -> (node.value as EntityNode.Entity).entity.type == exclude }
                .toList()
                .forEach(Tree<EntityNode>::delete)
        }

        this.tree.maxDepth?.let { depth ->
            tree.walk()
                .filter { it.depth == depth + 1 }
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
        // avoid styling sift template as it is a special case relating to not
        // yet having a good way to deal with elements inherited by multiple classes
        if (template.template is SiftSelfTemplate)
            return

        fun e(node: Tree<EntityNode>?): EntityNode.Entity? {
            return when (val v = node?.value) {
                is EntityNode.Entity -> v
                is EntityNode.Label  -> null
                null                 -> null
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

    private fun buildTree(roots: List<Entity.Type>): Pair<SystemModel, Tree<EntityNode>> {
        val template = this.template.template!!

        val sm: SystemModel = resolveClassNodes(this.template.classNodes!!, this.template.mavenRepositories)
            .let(::TemplateProcessor)
            .execute(template.template(), this.template.profile)

        return sm to template.toTree(sm, roots)
    }

    private fun buildTree(sm: SystemModel, roots: List<Entity.Type>): Tree<EntityNode> {
        return template.template!!.toTree(sm, roots)
    }

    fun toString(template: SystemModelTemplate): String {
        val types = stylizedEntityTypes(template)
        return "${fg("entity types of ")}${(fg + bold)(template.name)}\n" + types.values
            .joinToString(separator = "\n") { label -> "${fg("-")} $label" }
    }

    fun toString(template: SystemModelTemplate, pr: SystemModel): String {
        val types = stylizedEntityTypes(template)
        return "${fg("entity types of ")}${(fg + bold)(template.name)}\n" + types
            .map { (type, label) -> pr[type].size.toString().padStart(3) to label }
            .joinToString(separator = "\n") { (count, id) -> "${(fg + bold)(count)} $id" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun stylizedEntityTypes(template: SystemModelTemplate): Map<Entity.Type, String> {
        return tree("") {
            template
                .entityTypes
                .sortedBy(Entity.Type::id)
                .map { Entity(it, it.id) }
                .forEach(::add)
        }.also { stylize(it, template.theme()) }
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

        val uuid = uuidSequence()

        val t = Tree(fg("entities"))
        fun addEntity(e: Entity) {
            t.add(Tree(fg("Entity[${green2(e.label.take(80))}]")).apply {
                entry(this, 20, "id", uuid(e.id.toString()))
                entry(this, 20, "type", e.type, orange1)
                add("children").let { children ->
                    e.children().forEach { child ->
                        entry(children, 17, child, e.children(child).joinToString { uuid(it.id.toString()) })
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
        val (sm, _) = buildTree(tree.treeRoot)
        fun MeasurementScope.style(): TextStyle = when (this) {
            MeasurementScope.Template    -> fg
            MeasurementScope.Class       -> aqua2
            MeasurementScope.Field       -> blue2
            MeasurementScope.Method      -> green2
            MeasurementScope.Parameter   -> purple2
            MeasurementScope.FromContext -> red2 // shouldn't happen often
            MeasurementScope.Signature   -> orange2
            MeasurementScope.TypeErased  -> blue1 + bold
        }

        val gradient = listOf(dark4, gray, light3, yellow1, yellow2, red1, red2)

        var lastEntityCount = 0

        // print headers
        terminal.println((fg + bold)("     exec  ety#    in      out"))
        terminal.println(sm.measurements.toString(
            format = { measurement ->
                measurement.scopeIn.style()(measurement.action)
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
}

private fun numberFormatter(): (Any) -> String {
    val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
    formatter.decimalFormatSymbols
        .also { it.groupingSeparator = ' ' }
        .let(formatter::setDecimalFormatSymbols)
    val format: (Any) -> String = formatter::format
    return format
}

private fun loadMetadata(): (String) -> String {
    val props = Properties()
    SiftCli::class.java
        .getResourceAsStream("/sift-metadata.properties")
        .use(props::load)

    return props::getProperty
}

fun templateNames() = templates().map { (name, _)  -> name }

fun templates(): Map<String, () -> SystemModelTemplate> {
    val fromSpi = ServiceLoader
        .load(SystemModelTemplateServiceProvider::class.java)
        .map(SystemModelTemplateServiceProvider::create)
        .map { it.name to { it } }
        .toMap()

    // FIXME: windows paths
    val fromUserLocal = File("${System.getProperty("user.home")}/.local/share/sift/templates")
        .also(File::mkdirs)
        .listFiles()!!
        .filter { it.extension == "json" }
        .map { file -> file.nameWithoutExtension to { SystemModelTemplate.deserialize(file.readText()) } }

    return (fromSpi + fromUserLocal).toSortedMap()
}

val defaultStyle = Style.Plain(fg)

fun main(args: Array<String>) {
    SiftCli.completionOption().main(args)
}
