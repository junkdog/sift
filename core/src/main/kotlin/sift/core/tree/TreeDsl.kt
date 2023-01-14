package sift.core.tree

import sift.core.entity.Entity

class TreeDsl(private val hosted: Tree<EntityNode>) {

    fun add(label: String, f: TreeDsl.() -> Unit) {
        add(EntityNode.Label(label), f)
    }

    fun add(entity: Entity, f: TreeDsl.() -> Unit = {}) {
        add(EntityNode.Entity(entity, entity.label), f)
    }

    fun add(entity: Tree<EntityNode>) {
        hosted.add(entity)
    }

    private fun add(node: EntityNode, f: TreeDsl.() -> Unit) {
        hosted.add(node)
            .let(::TreeDsl)
            .also(f)
    }

    fun selfReferential(entity: Entity): Boolean {
        return hosted.parents()
            .mapNotNull { it.value as? EntityNode.Entity }
            .map(EntityNode.Entity::entity)
            .let { parents -> entity in parents }
    }

    companion object  {
        fun treeOf(
            entities: Iterable<Entity>,
            label: String = "",
        ): Tree<EntityNode> {
            return tree(label) {
                entities.forEach(::add)
            }
        }

        fun tree(label: String, f: TreeDsl.() -> Unit): Tree<EntityNode> {
            return TreeDsl(Tree(EntityNode.Label(label)))
                .also(f)
                .hosted
        }

        fun tree(delegated: Tree<EntityNode>, f: TreeDsl.() -> Unit): Tree<EntityNode> {
            return TreeDsl(delegated)
                .also(f)
                .hosted
        }
    }
}

fun TreeDsl.buildTree(
    e: Entity,
    vararg exceptChildren: String = arrayOf("sent-by", "backtrack")
) {
    (e.children() - exceptChildren.toSet()).forEach { key ->
        e.children(key).forEach { child: Entity ->
            // FIXME: selfReferential is a bug in establishing relations
            if (!selfReferential(child)) {
                add(child) { buildTree(child) }
            }
        }
    }
}
