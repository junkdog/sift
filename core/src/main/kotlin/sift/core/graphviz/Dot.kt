package sift.core.graphviz

import com.github.ajalt.mordant.rendering.TextStyle
import net.onedaybeard.collectionsby.filterBy
import net.onedaybeard.collectionsby.firstBy
import sift.core.api.SystemModel
import sift.core.entity.Entity
import sift.core.pop
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.terminal.Gruvbox
import sift.core.terminal.TextTransformer
import sift.core.terminal.StringEditor

/*

dot-id-as            entity.type
dot-edge-label       str; for edge in conj with `dot-id-as`
dot-type             edge|node
dot-ignore           true|false
dot-rank             0..MAX
dot-arrowhead        onormal|..
dot-style            dashed|..
dot-label-transform  TextTransformer..

dot-shape            folder|box3d|cylinder|component|..

~~dot-label-strip      suffix~~
 */


@Suppress("EnumEntryName", "unused")
enum class EdgeLayout {
    spline, polyline, ortho
}

class DiagramGenerator(
    sm: SystemModel,
    private val edgeSplines: EdgeLayout,
    private val colorLookup: (Entity.Type) -> String
) {
    private val nodes: List<Entity> = sm.entitiesByType
        .values
        .flatten()
        .filterBy(Entity::dotType, Dot.node)

    private val Entity.color: String
        get() = colorLookup(type)

    fun build(tree: Tree<EntityNode>): String {

        // we always want to render the --root entity type as nodes,
        // even if they are marked as Dot.edge
        val rootEntities = tree.children()
            .filter(Tree<EntityNode>::validNode)
            .mapNotNull(Tree<EntityNode>::entity)
            .filter { e -> e["dot-type"]?.firstOrNull() == Dot.edge }
            .toSet()

        val includedNodes = tree.walk()
            .filter(Tree<EntityNode>::validNode)
            .mapNotNull(Tree<EntityNode>::entity)
            .map(Entity::nodeId)
            .toSet()

        val nodes = (rootEntities + nodes)
            .toSet()
            .filter { it.nodeId in includedNodes }

        val ortho = " splines=${edgeSplines.name},"
        return """
            |digraph {
            |    // setup
            |    graph [rankdir=LR,$ortho truecolor=true, bgcolor="#00000000", margin=0.2, nodesep=0.2, ranksep=0.2];
            |    node [
            |        shape=box;
            |        fontname="verdana";
            |        fontcolor="#ebdbb2";
            |    ];
            |    edge [
            |        arrowhead=normal;
            |        arrowtail=dot;
            |        fontcolor="#ebdbb2";
            |        fontname="verdana";
            |        fontsize=11;
            |    ];
            |
            |    // nodes
            |    ${nodes(nodes)}
            |    
            |    // node ranks
            |    ${ranks(nodes)}
            |    
            |    // graph
            |    ${graph(tree, edgeSplines)}
            |}
        """.trimMargin()
    }

    private fun nodes(entities: List<Entity>): String {
        fun describe(e: Entity): String {
            val shape = e.dotShape?.let { ",shape=$it" } ?: ""
            return "${e.nodeId}[label=\"${e.dotLabel}\",color=\"${e.color}\"$shape];"
        }
        return entities.map(::describe).joinToString("\n    ")
    }
}

private fun ranks(entities: List<Entity>): String {
    return entities
        .groupBy(Entity::dotRank)
        .toSortedMap()
        .map { (_, same) -> "{ rank=same; ${same.map(Entity::nodeId).joinToString(", ")} };" }
        .joinToString("\n    ")
}

private fun graph(tree: Tree<EntityNode>, edgeSplines: EdgeLayout): String {
    val paths = tree.walk()
        .filter { it.children().isEmpty() }
        .map { leaf -> listOf(leaf) + leaf.parents() }
        .toList()
        .map { path -> path.filter(Tree<EntityNode>::validNode) }
        .filter { it.size > 1 }

    val colors = edgeColors()
    val edges = paths
        .map { path -> path.takeLast(2).first() }
        .toSet()

    fun colorOf(path: List<Tree<EntityNode>>) =
        colors[edges.indexOf(path.takeLast(2).first()) % colors.size]

    return paths
        .flatMap { path -> graph(path, colorOf(path)) }
        .map { relation -> toDotString(relation, edgeSplines) }
        .toSet()
        .joinToString("\n    ")
}

private fun graph(
    path: List<Tree<EntityNode>>,
    color: TextStyle
): List<Relation> {
    val remaining = path.toMutableList()

    val relations = mutableListOf<Relation>()
    var relation = Relation(color = color.hexColor)

    while (remaining.isNotEmpty()) {
        if (remaining.last().value is EntityNode.Label) {
            remaining.pop()
            continue
        }

        val current = remaining.pop().entity!!
        when {
            relation.from == null -> relation.from = current
            current.dotType == Dot.edge -> relation.transit += current
            else -> {
                relation.to = current
                relations += relation

                relation = Relation(from = current, color = color.hexColor)
            }
        }
    }

    return relations
}

private fun toDotString(relation: Relation, edgeSplines: EdgeLayout): String {
    val labelType = if (edgeSplines == EdgeLayout.ortho) "xlabel" else "label"

    val label = (relation.transit
        .map(Entity::dotLabel)
        .joinToString("\\n")
        .takeIf(String::isNotEmpty)
        ?: relation.to!!.dotEdgeLabel
    )
    // reserve some space for labels and min spacing
    val minlen = "minlen="
        .takeIf { edgeSplines == EdgeLayout.ortho }
        ?.takeIf { label != null }
        ?.let { it + (label!!.length / 2 + 2) }
        ?: "minlen=2"

    val arrowhead = relation.transit
        .map(Entity::dotArrowhead)
        .firstOrNull()
        ?.let { "arrowhead=$it" }

    val style = relation.transit
        .map(Entity::dotStyle)
        .firstOrNull()
        ?.let { "style=$it" }

    val attributes = listOfNotNull(
        label?.let { """$labelType="$it"""" },
        minlen,
        arrowhead,
        style
    ).joinToString("") { "$it," }

    return "${relation.from!!.nodeId} -> ${relation.to!!.nodeId}[${attributes}color=\"${relation.color}\"];"
}

private data class Relation(
    var from: Entity? = null,
    var to: Entity? = null,
    var transit: MutableList<Entity> = mutableListOf(),
    var color: String
)

private fun Tree<EntityNode>.validNode(): Boolean {
    val e = entity ?: return false
    return e["dot-type"] != null
        || e["dot-id-as"] != null
}

private operator fun java.lang.StringBuilder.plusAssign(s: String) {
    append(s)
}

private val Tree<EntityNode>.entity: Entity?
    get() = (value as? EntityNode.Entity)?.entity

private val Entity.nodeId: String
    get() = when(val type = get("dot-id-as")?.firstOrNull() as Entity.Type?) {
        null -> label.replace(Regex("[\\s/|\\[\\]:(){}.\\-$]"), "_")
        else -> children("backtrack").firstBy(Entity::type, type).nodeId
    }

private val Entity.dotRank: Int
    get() = this["dot-rank"]?.firstOrNull() as Int? ?: 0

private val Entity.dotLabel: String
    get() = dotLabelTransformers.fold(label) { s, transform -> transform(s) }

@Suppress("UNCHECKED_CAST")
private val Entity.dotLabelTransformers: List<TextTransformer>
    get() = (this["dot-label-transform"] as Iterable<StringEditor>?)
        ?.firstOrNull()
        ?.transformers ?: listOf()

private val Entity.dotArrowhead: String?
    get() = this["dot-arrowhead"]?.firstOrNull() as String?

private val Entity.dotStyle: String?
    get() = this["dot-style"]?.firstOrNull() as String?

private val Entity.dotType: Dot?
    get() = this["dot-type"]?.firstOrNull() as Dot?

private val Entity.dotShape: String?
    get() = this["dot-shape"]?.firstOrNull() as String?

private val Entity.dotEdgeLabel: String?
    get() = this["dot-edge-label"]?.firstOrNull() as String?

private fun edgeColors() = listOf(
    Gruvbox.aqua1,
    Gruvbox.blue1,
    Gruvbox.fg,
    Gruvbox.gray244,
    Gruvbox.green1,
    Gruvbox.orange1,
    Gruvbox.purple1,
    Gruvbox.red1,
    Gruvbox.yellow1,
).shuffled() + listOf(
    Gruvbox.aqua2,
    Gruvbox.blue2,
    Gruvbox.gray245,
    Gruvbox.green2,
    Gruvbox.orange2,
    Gruvbox.purple2,
    Gruvbox.red2,
    Gruvbox.yellow2,
).shuffled()

private val TextStyle.hexColor
    get() = color!!.toSRGB().toHex()

@Suppress("EnumEntryName")
enum class Dot {
    edge, node
}

@Suppress("EnumEntryName", "unused")
enum class Style {
    dashed, dotted, bold, filled, rounded
}

@Suppress("EnumEntryName", "unused")
enum class Shape {
    assembly,
    box,
    box3d,
    cds,
    circle,
    component,
    cylinder,
    diamond,
    doublecircle,
    doubleoctagon,
    egg,
    ellipse,
    fivepoverhang,
    folder,
    hexagon,
    house,
    insulator,
    invhouse,
    invtrapezium,
    invtriangle,
    larrow,
    lpromoter,
    Mcircle,
    Mdiamond,
    Msquare,
    none,
    note,
    noverhang,
    octagon,
    oval,
    parallelogram,
    pentagon,
    plain,
    plaintext,
    point,
    polygon,
    primersite,
    promoter,
    proteasesite,
    proteinstab,
    rarrow,
    rect,
    rectangle,
    restrictionsite,
    ribosite,
    rnastab,
    rpromoter,
    septagon,
    signature,
    square,
    star,
    tab,
    terminator,
    threepoverhang,
    trapezium,
    triangle,
    tripleoctagon,
    underline,
    utr,
}