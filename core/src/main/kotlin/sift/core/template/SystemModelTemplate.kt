package sift.core.template

import sift.core.api.Action
import sift.core.api.SystemModel
import sift.core.entity.Entity
import sift.core.terminal.Style
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.core.tree.addEntity
import sift.core.tree.buildTree

interface SystemModelTemplate {
    val name: String
    val description: String
    val defaultType: Entity.Type
    val entityTypes: Iterable<Entity.Type>

    fun template(): Action<Unit, Unit>
    fun theme(): Map<Entity.Type, Style>

    fun toTree(sm: SystemModel, roots: List<Entity.Type>): Tree<EntityNode> {
        return sm.toTree(roots.takeIf { it.isNotEmpty() } ?: listOf(defaultType))
    }

    companion object
}

fun SystemModel.toTree(roots: List<Entity.Type>): Tree<EntityNode> {
    return tree(roots.joinToString(" + ")) {
        roots.forEach { root ->
            this@toTree[root].forEach { e ->
                addEntity(e) {
                    buildTree(e)
                }
            }
        }
    }.also { it.sort(compareBy(EntityNode::toString)) }
}
