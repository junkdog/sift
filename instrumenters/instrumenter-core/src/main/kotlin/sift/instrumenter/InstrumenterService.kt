package sift.instrumenter

import sift.core.api.Action
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.instrumenter.spi.InstrumenterServiceProvider

interface InstrumenterService : InstrumenterServiceProvider {
    val name: String
    val entityTypes: Iterable<Entity.Type>

    fun pipeline(): Action<Unit, Unit>
    fun toTree(es: EntityService, forType: Entity.Type?): Tree<EntityNode>
    fun theme(): Map<Entity.Type, Style>

    companion object {}
}
