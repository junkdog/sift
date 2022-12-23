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

/*

dot-id-as           entity.type
dot-edge-label      str; for edge in conj with `dot-id-as`
dot-label-strip     suffix
dot-type            edge|node
dot-ignore          true|false
dot-rank            0..MAX
dot-arrowhead       onormal|..
dot-style           dashed|..

dot-shape           folder|box3d|cylinder|component|..


 */


class DiagramGenerator(
    sm: SystemModel,
    val colorLookup: (Entity.Type) -> String
) {
    private val nodes: List<Entity> = sm.entitiesByType
        .values
        .flatten()
        .filterBy(Entity::dotType, Dot.node)

    init {
        // move lingering dot-id to dot-id-as (todo: remove)
        sm.entitiesByType
            .values
            .flatten()
            .forEach { e -> e["dot-id"]?.firstOrNull()?.let { e["dot-id-as"] = it }  }
    }

    private val Entity.color: String
        get() = colorLookup(type)

    fun build(tree: Tree<EntityNode>): String {

        // filter only dot nodes and edges
        tree.walk()
            .filterNot(Tree<EntityNode>::validEntity)
            .forEach(Tree<EntityNode>::delete)


        // we always want to render the --root entity type as nodes,
        // even if they are marked as Dot.edge
        val rootEntities = tree.children()
            .mapNotNull(Tree<EntityNode>::entity)
            .filter { e -> e["dot-type"]?.firstOrNull() == Dot.edge }
            .toSet()

        val includedNodes = tree.walk()
            .mapNotNull(Tree<EntityNode>::entity)
            .map(Entity::nodeId)
            .toSet()

        val nodes = (rootEntities + nodes)
            .toSet()
            .filter { it.nodeId in includedNodes }

        val ortho = "" // " splines=ortho,"
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
            |    ${graph(tree)}
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

private fun graph(tree: Tree<EntityNode>): String {
    val paths = tree.walk()
        .filter { it.children().isEmpty() }
        .map { leaf -> listOf(leaf) + leaf.parents() }
        .toList()
        .map { path -> path.filterNot { it.entity == null || "dot-ignore" in it.entity!!.properties() } }

    val colors = edgeColors()
    val edges = paths
        .map { path -> path.takeLast(2).first() }
        .toSet()

    fun colorOf(path: List<Tree<EntityNode>>) =
        colors[edges.indexOf(path.takeLast(2).first()) % colors.size]

    return paths
        .flatMap { path -> graph(path, colorOf(path)) }
        .map { relation -> toDotString(relation) }
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

private fun toDotString(relation: Relation): String {
    val label = (relation.transit
        .map(Entity::dotLabel)
        .joinToString("\\n")
        .takeIf(String::isNotEmpty)
        ?: relation.to!!.dotEdgeLabel
    )?.let { """label="$it"""" }

    val arrowhead = relation.transit
        .map(Entity::dotArrowhead)
        .firstOrNull()
        ?.let { "arrowhead=$it" }

    val style = relation.transit
        .map(Entity::dotStyle)
        .firstOrNull()
        ?.let { "style=$it" }

    val attributes = listOfNotNull(label, arrowhead, style).joinToString("") { "$it," }

    return "${relation.from!!.nodeId} -> ${relation.to!!.nodeId}[${attributes}color=\"${relation.color}\"];"
}

private data class Relation(
    var from: Entity? = null,
    var to: Entity? = null,
    var transit: MutableList<Entity> = mutableListOf(),
    var color: String
)

private fun Tree<EntityNode>.validEntity(): Boolean {
    val e = entity ?: return false
    return e["dot-type"] != null
        || e["dot-id"] != null
        || e["dot-ignore"]?.firstOrNull() == true // not rendered
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
    get() = label.removeSuffix((this["dot-label-strip"]?.firstOrNull() as String?) ?: "")

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

enum class Dot {
    edge, node
}