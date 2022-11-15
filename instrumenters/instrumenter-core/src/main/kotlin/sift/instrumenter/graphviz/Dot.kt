package sift.instrumenter.graphviz

import com.github.ajalt.mordant.rendering.TextStyle
import net.onedaybeard.collectionsby.filterBy
import net.onedaybeard.collectionsby.firstBy
import sift.core.anyOf
import sift.core.api.SystemModel
import sift.core.entity.Entity
import sift.core.graphviz.Dot
import sift.core.pop
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.instrumenter.Gruvbox
import sift.instrumenter.toTree

/*

dot-id              entity.type
dot-label-strip     suffix
dot-type            edge|node
dot-ignore          true|false
dot-rank            0..MAX
dot-arrowhead       onormal|..
dot-style           dashed|..

 */


// register + lookup for creation
class GraphContext(
    val sm: SystemModel,
    val tree: Tree<EntityNode>,
    val colorLookup: (Entity.Type) -> String
) {
    private val nodes: List<Entity> = sm.entitiesByType
        .values
        .flatten()
        .filterBy(Entity::dotType, Dot.node)

    private val edges: List<Entity> = sm.entitiesByType
        .values
        .flatten()
        .filterBy(Entity::dotType, Dot.edge)

    private val Entity.color: String
        get() = colorLookup(type)


    fun build(): String {
        return """
            |digraph {
            |    // setup
            |    graph [rankdir=LR, truecolor=true, bgcolor="#00000000", margin=0.2 nodesep=0.2, ranksep=0.2];
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
        fun describe(e: Entity) = "${e.nodeId}[label=\"${e.dotLabel}\",color=\"${e.color}\"];"
        return entities.map(::describe).joinToString("\n    ")
    }

    private fun ranks(entities: List<Entity>): String {
        return entities
            .groupBy(Entity::dotRank)
            .toSortedMap()
            .map { (_, same) -> "{ rank=same; ${same.map(Entity::nodeId).joinToString(", ")} };" }
            .joinToString("\n    ")
    }

    private fun graph(tree: Tree<EntityNode>): String {
        // filter only dot nodes and edges
        tree.walk()
            .filter { it.entity != null }
            .filter { it.entity!!["dot-type"] == null && it.entity!!["dot-id"] == null && it.entity!!["dot-ignore"]?.firstOrNull() != true }
            .forEach(Tree<EntityNode>::delete)

        val paths = tree.walk()
            .filter { it.children().isEmpty() }
            .map { leaf -> listOf(leaf) + leaf.parents() }
            .toList()

        val entitiesToSkip = anyOf<Tree<EntityNode>>(
            { it.entity == null }, // label node
            { "dot-ignore" in it.entity!!.properties() }
        )

        val colors = edgeColors()
        val graph = paths.flatMap { path ->
            val remaining = path
                .filterNot(entitiesToSkip)
                .toMutableList()

            val pathColor = colors[remaining.last().index % colors.size].hexColor

            val relations = mutableListOf<Relation>()
            var relation = Relation(color = pathColor)

            while (remaining.isNotEmpty()) {
                if (remaining.last().value is EntityNode.Label) {
                    remaining.pop()
                    continue
                }

                val current = remaining.pop().entity!!
                when {
                    relation.from == null       -> relation.from = current
                    current.dotType == Dot.edge -> relation.transit += current
                    else                        -> {
                        relation.to = current
                        relations += relation

                        relation = Relation(from = current, color = pathColor)
                    }
                }
            }

            relations
        }.map { relation ->
            val label = relation.transit
                .map(Entity::dotLabel)
                .joinToString("\\n")
                .takeIf(String::isNotEmpty)
                ?.let { """label="$it"""" }

            val arrowhead = relation.transit
                .map(Entity::dotArrowhead)
                .firstOrNull()
                ?.let { "arrowhead=$it" }

            val style = relation.transit
                .map(Entity::dotStyle)
                .firstOrNull()
                ?.let { "style=$it" }

            val attributes = listOfNotNull(label, arrowhead, style).joinToString("") { "$it," }

            "${relation.from!!.nodeId} -> ${relation.to!!.nodeId}[${attributes}color=\"${relation.color}\"];"
        }

        return graph.toSet().joinToString("\n    ")
    }
}

private data class Relation(
    var from: Entity? = null,
    var to: Entity? = null,
    var transit: MutableList<Entity> = mutableListOf(),
    var color: String
)

private operator fun java.lang.StringBuilder.plusAssign(s: String) {
    append(s)
}

private val Tree<EntityNode>.entity: Entity?
    get() = (value as? EntityNode.Entity)?.entity

private val Tree<EntityNode>.nodeId: String
    get() = entity!!.nodeId

private val Tree<EntityNode>.dotRank: Int
    get() = entity!!.dotRank

private val Tree<EntityNode>.dotType: Dot?
    get() = entity!!.dotType

private val Entity.nodeId: String
    get() = when(val type = get("dot-id")?.firstOrNull() as Entity.Type?) {
        null -> label.replace(Regex("[\\s/|\\[\\]:(){}-]"), "_")
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

private fun edgeColors() = listOf(
    Gruvbox.aqua1,
    Gruvbox.aqua2,
    Gruvbox.blue1,
    Gruvbox.blue2,
    Gruvbox.fg,
    Gruvbox.gray244,
    Gruvbox.gray245,
    Gruvbox.green1,
    Gruvbox.green2,
    Gruvbox.orange1,
    Gruvbox.orange2,
    Gruvbox.purple1,
    Gruvbox.purple2,
    Gruvbox.red1,
    Gruvbox.red2,
    Gruvbox.yellow1,
    Gruvbox.yellow2,
).shuffled()

val TextStyle.hexColor
    get() = color!!.toSRGB().toHex()