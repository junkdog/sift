package sift.core.tree

import sift.core.entity.Entity
import sift.core.terminal.TextTransformer
import sift.core.terminal.TextTransformer.Companion.uuidSequence

class TreeDsl<T>(internal val hosted: Tree<T>, val uuidToId: TextTransformer) {

    fun add(entity: Tree<T>) {
        hosted.add(entity)
    }

    fun TreeDsl<T>.add(node: T, f: TreeDsl<T>.() -> Unit) {
        hosted.add(node)
            .let { TreeDsl(it, uuidToId) }
            .also(f)
    }

    companion object  {
        fun treeOf(
            entities: Iterable<Entity>,
            label: String = "",
        ): Tree<EntityNode> {
            return tree(label) {
                entities.forEach(::addEntity)
            }
        }

        fun tree(label: String, f: TreeDsl<EntityNode>.() -> Unit): Tree<EntityNode> {
            return TreeDsl<EntityNode>(Tree(EntityNode.Label(label)), uuidSequence())
                .also(f)
                .hosted
        }
    }
}



fun TreeDsl<EntityNode>.selfReferential(entity: Entity): Boolean {
    val refId = listOfNotNull((hosted.parent?.value as? EntityNode.Entity)?.entity, entity)
    return hosted.parents()
        .mapNotNull { it.value as? EntityNode.Entity }
        .map(EntityNode.Entity::entity)
        .windowed(2)
        .any { parentPair -> parentPair == refId }
}

fun TreeDsl<EntityNode>.add(label: String, f: TreeDsl<EntityNode>.() -> Unit) {
    add(EntityNode.Label(label), f)
}

fun TreeDsl<EntityNode>.addEntity(entity: Entity, f: TreeDsl<EntityNode>.() -> Unit = {}) {
    EntityNode.Entity(entity, entity.label)
        .also { e -> e["id"] = uuidToId(entity.id) }
        .also { e -> e["element-id"] = entity["element-id"]!!.first() }
        .also { e -> e["element-type"] = entity["element-type"]!!.first() }
        .also { e -> e["entity-type"] = entity.type }
        .also { e -> add(e, f) }
}

fun TreeDsl<EntityNode>.buildTree(
    e: Entity,
    vararg exceptChildren: String = arrayOf("sent-by", "backtrack")
) {
    (e.children() - exceptChildren.toSet()).forEach { key ->
        e.children(key).forEach { child: Entity ->
            if (!selfReferential(child)) {
                addEntity(child) { buildTree(child) }
            }
        }
    }
}
