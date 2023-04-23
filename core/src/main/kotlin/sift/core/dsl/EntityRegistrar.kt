package sift.core.dsl

import kotlinx.coroutines.FlowPreview
import sift.core.api.Action
import sift.core.api.Iter
import sift.core.element.Element
import sift.core.entity.Entity
import sift.core.entity.LabelFormatter
import sift.core.terminal.TextTransformer

interface EntityRegistrar<ELEMENT : Element> {

    /**
     * This entity tracks [children] under the label denoted by [key].
     *
     * ## Example
     * ```
     * classes {
     *     filter(Regex("^sift\\.core\\.api\\.testdata"))
     *     annotatedBy<RestController>()
     *     entity(controller)
     *     methods {
     *         annotatedBy<Endpoint>()
     *         entity(endpoint, label("\${http-method} \${path}"),
     *             property("http-method", readAnnotation(Endpoint::method)),
     *             property("path", readAnnotation(Endpoint::path)))
     *
     *         // relating endpoints to controllers
     *         controller["endpoints"] = endpoint
     *     }
     * }
     * ```
     */
    operator fun Entity.Type.set(
        key: String,
        children: Entity.Type
    )

    /** new entity with label from [labelFormatter] */
    fun entity(
        id: Entity.Type,
        labelFormatter: LabelFormatter,
        errorIfExists: Boolean = true,
        vararg properties: Property<ELEMENT>
    )

    /** register entity */
    fun entity(
        id: Entity.Type,
        vararg properties: Property<ELEMENT>
    ) {
        entity(id, LabelFormatter.FromElement, true, *properties)
    }

    /** register entity */
    fun entity(
        id: Entity.Type,
        labelFormatter: LabelFormatter,
        vararg properties: Property<ELEMENT>
    ) {
        entity(id, labelFormatter, true, *properties)
    }

    /** new entity with label inferred from introspected bytecode element */
    fun entity(
        id: Entity.Type,
        errorIfExists: Boolean = true,
        vararg properties: Property<ELEMENT>
    ) {
        entity(id, LabelFormatter.FromElement, errorIfExists, *properties)
    }


    /**
     * Set the [entity] label from a [pattern], replacing any `${}` variables
     * inside [pattern] with values from [Entity.properties].
     *
     * ## Example: jdbi3
     *
     * Entity labels from SQL inside `@SqlUpdate` annotation
     *
     * ```
     * methods {
     *     annotatedBy(A.sqlUpdate)
     *     entity(E.sqlUpdate, label("\${sql}"),
     *         property("sql", readAnnotation(A.sqlUpdate, "value"))
     *     )
     * }
     * ```
     */
    fun label(
        pattern: String,
        vararg ops: TextTransformer
    ): LabelFormatter = LabelFormatter.FromPattern(pattern, ops.toList())

    companion object {
        fun <ELEMENT : Element> scopedTo(
            action: Action.Chain<Iter<ELEMENT>>
        ): EntityRegistrar<ELEMENT> = EntityRegistrarImpl(action)
    }
}

@OptIn(FlowPreview::class)
private class EntityRegistrarImpl<ELEMENT : Element>(
    val action: Action.Chain<Iter<ELEMENT>>
) : EntityRegistrar<ELEMENT> {

    override operator fun Entity.Type.set(
        key: String,
        children: Entity.Type
    ) {
        action += Action.RegisterChildren(this, key, children)
    }

    override fun entity(
        id: Entity.Type,
        labelFormatter: LabelFormatter,
        errorIfExists: Boolean,
        vararg properties: Property<ELEMENT>
    ) {
        action += Action.RegisterEntity(id, errorIfExists, labelFormatter)

        if (properties.isNotEmpty()) {
            action += properties
                .mapNotNull(Property<ELEMENT>::action)
                .map { Action.Fork(it) as Action<Iter<ELEMENT>, Iter<ELEMENT>> }
                .reduce { acc, action -> acc andThen action }
        }
    }
}