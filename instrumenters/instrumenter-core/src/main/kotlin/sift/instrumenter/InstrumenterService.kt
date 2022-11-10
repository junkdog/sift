package sift.instrumenter

import sift.core.api.Action
import sift.core.api.PipelineResult
import sift.core.entity.Entity
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.instrumenter.dsl.buildTree
import sift.instrumenter.spi.InstrumenterServiceProvider

interface InstrumenterService : InstrumenterServiceProvider {
    val name: String
    val defaultType: Entity.Type
    val entityTypes: Iterable<Entity.Type>

    fun pipeline(): Action<Unit, Unit>
    fun theme(): Map<Entity.Type, Style>

    fun toTree(es: PipelineResult, forType: Entity.Type?): Tree<EntityNode> {
        val type = forType ?: defaultType
        return tree(type.id) {
            es[type].forEach { e ->
                add(e) {
                    buildTree(e)
                }
            }
        }.also { it.sort(compareBy(EntityNode::toString)) }
    }

    companion object
}
