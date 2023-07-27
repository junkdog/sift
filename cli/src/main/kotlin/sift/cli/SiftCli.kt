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
import sift.core.entity.Entity
import sift.core.graphviz.DiagramGenerator
import sift.core.jackson.*
import sift.core.template.SystemModelTemplate
import sift.core.template.deserialize
import sift.core.terminal.*
import sift.core.terminal.Gruvbox.aqua1
import sift.core.terminal.Gruvbox.aqua2
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.dark2
import sift.core.terminal.Gruvbox.dark3
import sift.core.terminal.Gruvbox.fg
import sift.core.terminal.Gruvbox.gray
import sift.core.terminal.Gruvbox.green2
import sift.core.terminal.Gruvbox.light0
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.terminal.Gruvbox.purple2
import sift.core.terminal.Gruvbox.red2
import sift.core.terminal.Style.Companion.diff
import sift.core.terminal.TextTransformer.Companion.uuidSequence
import sift.core.tree.*
import sift.core.tree.DiffNode.State
import sift.core.tree.DiffNode.State.Unchanged
import sift.core.tree.TreeDsl.Companion.tree
import sift.core.tree.TreeDsl.Companion.treeOf
import sift.template.*
import sift.template.spi.SystemModelTemplateServiceProvider
import java.io.File
import java.net.URI
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.max
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
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
        
          ${fg("sift -t spring-axon -f . -F \"Order(Created|Shipped)\"")}
          Model the system using the "spring-axon" template on the current directory's
          classes, filter nodes containing the regular expression "Order(Created|Shipped)".
        
          ${fg("sift -t spring-axon -f . --diff feature-01.json")}
          Compare the current design of the system using the "spring-axon" template on
          the classes in the current directory against a previously saved system model
          from "feature-01.json" and show the differences.
          
          ${fg("sift -t sift -f \"net.onedaybeard.sift:core:0.13.0\" --diff \"net.onedaybeard.sift:core:0.9.0\"")}
          Compare two different versions of the DSL API using the 'sift' template, specified
          by their maven coordinates.
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

    // todo: --column element-id
    val debugElementId: Boolean by option("--debug-element-id",
        help = "Print the elementId for each entity in the tree. Use the element id together with --debug-element-traces"
    ).flag()

    val debugInverseTrace: Boolean by option("--debug-inverse-trace",
        help = "Print the inverse element trace for each entity in the tree. Use together with --debug-element-traces"
    ).flag()

    val debugElementTraces: List<Int> by option("--debug-element-trace",
        help = "Print all element traces leading to the specified element ids",
        metavar = "ELEMENT_ID"
    ).int().multiple()

    val mavenRepositories: List<URI> by option("-m", "--maven-repository",
            help = "Additional maven repositories to use for downloading artifacts. Maven central " +
                   "(https://repo1.maven.org/maven2/) and local user repositories are always included.")
        .convert { URI(it) }
        .multiple()

    val statistics: Boolean by option("--statistics",
        help = "Print internal statistics about the system model template context."
    ).flag()

    private val noAnsi = Terminal(AnsiLevel.NONE)

    override fun run() {
        debugLog = debug

        val terminal = Terminal(ansi)
        val exceptionHandler = ExceptionHandler(terminal, stacktrace)
        Thread.setDefaultUncaughtExceptionHandler { _, e -> exceptionHandler(e) }

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
                        buildTree(tree.treeRoot).let { (sm, _) -> terminal.println(toString(template.template!!, sm)) }

                    serialization.load != null ->
                        terminal.println(toString(template.template!!, loadSystemModel(serialization.load!!)))

                    else -> terminal.println(toString(template.template!!))
                }
            }
            statistics && template.template != null -> {
                val sm = systemModel()
                sm.statistics().formatted
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
                val other = resolveSystemModel(template.diff!!, template.template, mavenRepositories, template.profile)
                val tree = diffHead(other, this.tree.treeRoot, template.template!!)
                terminal.printTree(tree)
            }
            serialization.load != null -> {
                val sm = loadSystemModel(serialization.load!!)
                val tree = template.template!!.toTree(sm, this.tree.treeRoot)

                terminal.printTree(tree)
            }
            debugElementTraces.isNotEmpty() -> {
                val tree = buildElementTraceTree(debugElementTraces.first())
                val formattedTree = tree.toString(
                    format = { node ->
                         when (node.type) {
                             "ClassNode"      -> aqua2
                             "MethodNode"     -> green2
                             "FieldNode"      -> blue2
                             "ParameterNode"  -> purple2
                             "SignatureNode"  -> orange2
                             "AnnotationNode" -> aqua1 + bold
                             else -> error("Unknown type: ${node.type}")
                         }(node.label)
                    },
                    prefix = { node ->
                        listOf(
                            dark2(node.type.replace("Node", "").lowercase().padEnd(10)),
                            aqua1((node.entityType?.toString() ?: "").padEnd(25)),
                            dark3(node.elementId.toString().padStart(5)),
                        ).joinToString("")
                    })

                when {
                    "SynthesisTemplate" in formattedTree -> formattedTree.lines().drop(1).joinToString("\n")
                    else -> formattedTree
                }.let(terminal::println)
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

    @OptIn(ExperimentalTime::class)
    fun systemModel(): SystemModel {
        return if (serialization.load != null) {
            loadSystemModel(serialization.load!!)
        } else {
            TemplateProcessor.from(template.classNodes!!, mavenRepositories)
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
            diffMerge(this, old.children(), new.children())
        }
    }

    private fun Terminal.printTree(
        tree: Tree<EntityNode>,
    ) {
        val theme = template.template!!.theme()
        stylize(tree, theme)
        filterTree(tree)
        backtrackStyling(tree, theme)

        println(tree.toString(entityNodeFormatter(), Columns::elementId))
    }

    object Columns {
        val all = allOf(listOf(
            Columns::elementId,
            Columns::elementType,
            Columns::entityType,
        ))

        fun allOf(columns: List<(EntityNode) -> String>): (EntityNode) -> String {
            return { node -> columns.joinToString(separator = " ") { it(node) } }
        }

        fun elementId(node: EntityNode): String {
            return dark3((node["element-id"]?.toString() ?: "").padStart(4, ' '))
        }

        fun entityType(node: EntityNode): String {
            return aqua1((node["entity-type"]?.toString() ?: "").padEnd(12, ' '))
        }

        fun elementType(node: EntityNode): String {
            return dark2((node["element-type"]?.toString() ?: "").padEnd(8, ' '))
        }
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
            format = entityNodeFormatter())
        )
    }

    private fun entityNodeFormatter(): EntityNode.() -> String {
        return when {
            tree.noEmoji -> { { toString().stripEmoji() } }
            else         -> EntityNode::toString
        }
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
        val t = this.template.template!!

        val sm: SystemModel = TemplateProcessor.from(template.classNodes!!, mavenRepositories)
            .execute(t.template(), template.profile)

        return sm to buildTree(sm, roots)
    }

    private fun buildElementTraceTree(elementId: Int): Tree<ElementNode> {
        val t = this.template.template!!

        val templateProcessor = TemplateProcessor.from(template.classNodes!!, mavenRepositories)
        templateProcessor
            .execute(t.template(), template.profile)

        return templateProcessor.traceElementId(elementId, debugInverseTrace)
    }

    private fun buildTree(sm: SystemModel, roots: List<Entity.Type>): Tree<EntityNode> {
        return template.template!!.toTree(sm, roots)
    }

    fun toString(template: SystemModelTemplate): String {
        val types = stylizedEntityTypes(template)
        return "${fg("entity types of ")}${(fg + bold)(template.name)}\n" + types.values
            .joinToString(separator = "\n") { label -> "${fg("-")} $label" }
    }

    fun toString(template: SystemModelTemplate, sm: SystemModel): String {
        val types = stylizedEntityTypes(template)
        return "${fg("entity types of ")}${(fg + bold)(template.name)}\n" + types
            .map { (type, label) -> sm[type].size.toString().padStart(3) to label }
            .joinToString(separator = "\n") { (count, id) -> "${(fg + bold)(count)} $id" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun stylizedEntityTypes(template: SystemModelTemplate): Map<Entity.Type, String> {
        return tree("") {
            template
                .entityTypes
                .sortedBy(Entity.Type::id)
                .map { Entity(it, it.id) }
                .forEach(::addEntity)
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
        printProfile(terminal, sm.measurements)
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

val Map<String, Int>.formatted: String
    get() {
        val format: (Any) -> String = numberFormatter()
        return entries.joinToString("\n") { (k, v) -> "%-40s %8s".format(k, format(v)) }
    }

val defaultStyle = Style.Plain(fg)

fun main(args: Array<String>) {
    SiftCli.completionOption().main(args)
}
