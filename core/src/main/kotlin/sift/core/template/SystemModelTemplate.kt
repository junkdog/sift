package sift.core.template

import sift.core.api.Action
import sift.core.api.SystemModel
import sift.core.entity.Entity
import sift.core.terminal.Style
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.core.tree.buildTree

interface SystemModelTemplate {
    val name: String
    val defaultType: Entity.Type
    val entityTypes: Iterable<Entity.Type>

    fun template(): Action<Unit, Unit>
    fun theme(): Map<Entity.Type, Style>

    fun toTree(sm: SystemModel, forType: Entity.Type?): Tree<EntityNode> {
        return sm.toTree(forType ?: defaultType)
    }

    companion object
}

fun SystemModel.toTree(root: Entity.Type): Tree<EntityNode> {
    return tree(root.id) {
        this@toTree[root].forEach { e ->
            add(e) {
                buildTree(e)
            }
        }
    }.also { it.sort(compareBy(EntityNode::toString)) }
}
