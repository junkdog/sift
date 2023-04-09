package sift.core.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.api.IterValues
import sift.core.element.Element
import sift.core.entity.Entity
import kotlin.reflect.KProperty1

abstract class Core<ELEMENT : Element>(
    internal val action: Action.Chain<Iter<ELEMENT>>,
) : EntityRegistrar<ELEMENT>         by EntityRegistrar.scopedTo(action),
    EntityPropertyRegistrar<ELEMENT> by EntityPropertyRegistrar.scopedTo(action)
{
    private var stack: MutableList<Action<*, *>> = mutableListOf()

    /**
     * Filters elements that belongs to [entity].
     */
    fun filter(entity: Entity.Type) {
        action += Action.EntityFilter(entity)
    }

    /**
     * When `--debug` is past to the CLI, prints [tag] and all elements
     * currently in scope.
     *
     * Note that for most use-cases, `--profile` yields better results
     * without having to modify the template.
     **/
    fun log(tag: String) {
        action += Action.DebugLog(tag)
    }

    /**
     * When `--debug` is past to the CLI, prints [tag] and the count
     * of elements currently in scope.
     *
     * Note that for most use-cases, `--profile` yields better results
     * without having to modify the template.
     **/
    fun logCount(tag: String) {
        action += Action.DebugLog(tag, format = Action.DebugLog.LogFormat.Count)
    }

    inline fun <reified T> annotatedBy() = annotatedBy(sift.core.dsl.type<T>())

    inline fun <reified T : Annotation> readAnnotation(
        field: KProperty1<T, *>
    ): Action<Iter<ELEMENT>, IterValues> = readAnnotation(type<T>(), field.name)
}
