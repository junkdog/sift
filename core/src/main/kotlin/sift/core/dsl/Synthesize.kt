package sift.core.dsl

import sift.core.api.Action
import sift.core.api.SiftTemplateDsl
import sift.core.api.chainFrom
import sift.core.entity.Entity
import sift.core.entity.LabelFormatter
import sift.core.terminal.TextTransformer

/**
 * This scope is used for stubbing classes not part of the input
 * classes.
 */
@SiftTemplateDsl
class Synthesize internal constructor(
    var action: Action.Chain<Unit> = chainFrom(Action.Template.TemplateScope)
) {
    /** Stub missing class node for [type] and register it to an entity */
    fun entity(
        id: Entity.Type,
        type: Type,
        labelFormatter: LabelFormatter = LabelFormatter.FromElement,
    ) {
        action += Action.RegisterSynthesizedEntity(id, type.asmType, labelFormatter)
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
    ) = LabelFormatter.FromPattern(pattern, ops.toList())
}