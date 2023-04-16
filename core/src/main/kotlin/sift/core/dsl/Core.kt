package sift.core.dsl

import sift.core.api.AccessFlags
import sift.core.api.Action
import sift.core.api.Iter
import sift.core.api.IterValues
import sift.core.element.Element
import sift.core.entity.Entity
import kotlin.reflect.KProperty1

abstract class Core<ELEMENT : Element>(
    internal val action: Action.Chain<Iter<ELEMENT>>,
    scope: AccessFlags.Scope,
) : EntityRegistrar<ELEMENT>         by EntityRegistrar.scopedTo(action),
    EntityPropertyRegistrar<ELEMENT> by EntityPropertyRegistrar.scopedTo(action),
    FilterableByAccessFlag<ELEMENT>  by FilterableByAccessFlag.scopedTo(action, scope),
    ElementDebugLogger<ELEMENT>      by ElementDebugLogger.scopedTo(action)
{
    private var stack: MutableList<Action<*, *>> = mutableListOf()

    /**
     * Filters elements that belongs to [entity].
     */
    fun filter(entity: Entity.Type) {
        action += Action.EntityFilter(entity)
    }

    inline fun <reified T> annotatedBy() = annotatedBy(type<T>())

    inline fun <reified T : Annotation> readAnnotation(
        field: KProperty1<T, *>
    ): Action<Iter<ELEMENT>, IterValues> = readAnnotation(type<T>(), field.name)
}
