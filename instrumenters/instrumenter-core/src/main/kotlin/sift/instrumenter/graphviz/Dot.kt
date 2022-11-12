package sift.instrumenter.graphviz

import net.onedaybeard.collectionsby.filterBy
import net.onedaybeard.collectionsby.firstBy
import sift.core.api.SystemModel
import sift.core.entity.Entity
import sift.core.pop
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.instrumenter.toTree

/*

dot-id              entity.type
dot-label-strip     suffix
dot-type            edge|node
dot-rank            0..MAX

 */


enum class Dot {
    edge, node
}

// register + lookup for creation
class GraphContext(
    val sm: SystemModel,
    val root: Entity.Type,
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
            |        fontcolor="#ababab";
            |    ];
            |    edge [
            |        arrowhead=normal;
            |        arrowtail=dot;
            |        fontcolor="#ababab";
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
            |    ${graph(sm.toTree(root))}
            |}
        """.trimMargin()
    }

    private fun nodes(entities: List<Entity>): String {
        fun describe(e: Entity) = "${e.nodeId}[label=\"${e.label}\",color=\"${e.color}\"];"
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
            .filter { (it.value as? EntityNode.Entity)?.entity?.get("sns-type") != null }
            .forEach(Tree<EntityNode>::delete)

        val paths = tree.walk()
            .filter { it.children().isEmpty() } // FIXME: ignore backtrack
            .map { leaf -> listOf(leaf) + leaf.parents() }
            .toList()

        val graph = paths.flatMap { path ->
            val remaining = path.toMutableList()

            val relations = mutableListOf<Relation>()
            var relation = Relation(color = "#ffffff")

            while (remaining.isNotEmpty()) {
                if (remaining.last().value is EntityNode.Label) {
                    remaining.pop()
                    continue
                }

                val current = remaining.pop().entity
                when {
                    relation.from == null       -> relation.from = current
                    current.dotType == Dot.edge -> relation.transit += current
                    else                        -> {
                        relation.to = current
                        relations += relation

                        relation = Relation(from = current, color = "#ffffff")
                    }
                }
            }

            relations
        }.map { relation ->
            when {
                relation.transit.isEmpty() -> {
                    "${relation.from!!.nodeId} -> ${relation.to!!.nodeId}[color=\"#ffffff\"];"
                }
                else -> {
                    val label = relation.transit.map(Entity::label).joinToString("\\n")
                    "${relation.from!!.nodeId} -> ${relation.to!!.nodeId}[label=\"$label\"color=\"#ffffff\"];"
                }
            }
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

private val Tree<EntityNode>.entity: Entity
    get() = (value as EntityNode.Entity).entity

private val Tree<EntityNode>.nodeId: String
    get() = entity.nodeId

private val Tree<EntityNode>.dotRank: Int
    get() = entity.dotRank

private val Tree<EntityNode>.dotType: Dot?
    get() = entity.dotType

private val Entity.nodeId: String
    get() = when(val type = get("dot-id")?.firstOrNull() as Entity.Type?) {
        null -> label.replace(Regex("[\\s/|\\[\\]:(){}-]"), "_")
        else -> children("backtrack").firstBy(Entity::type, type).nodeId
    }

private val Entity.dotRank: Int
    get() = this["dot-rank"]?.firstOrNull() as Int? ?: 0

private val Entity.dotType: Dot?
    get() = this["dot-type"]?.firstOrNull() as Dot?