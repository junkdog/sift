package sift.core.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.element.Element
import sift.core.entity.Entity

abstract class Core<ELEMENT : Element>(
    internal val action: Action.Chain<Iter<ELEMENT>>,
) : EntityRegistrar<ELEMENT>         by EntityRegistrar.scopedTo(action),
    EntityPropertyRegistrar<ELEMENT> by EntityPropertyRegistrar.scopedTo(action),
    ElementDebugLogger<ELEMENT>      by ElementDebugLogger.scopedTo(action)
{
    private var stack: MutableList<Action<*, *>> = mutableListOf()

    /**
     * Filters elements that belongs to [entity].
     */
    fun filter(entity: Entity.Type) {
        action += Action.EntityFilter(entity)
    }
}
