package sift.core.dsl

import sift.core.api.Action
import sift.core.api.SiftTemplateDsl
import sift.core.api.chainFrom
import sift.core.asm.classNode
import sift.core.asm.toBytes
import sift.core.entity.Entity
import sift.core.entity.LabelFormatter
import sift.core.terminal.TextTransformer
import kotlin.reflect.KClass

/**
 * This scope is used for stubbing classes not part of the input
 * classes.
 */
@SiftTemplateDsl
class Synthesize internal constructor(
    internal val action: Action.Chain<Unit> = chainFrom(Action.Template.TemplateScope)
) {
    /** Stub missing class node for [type] and register it to an entity */
    fun entity(
        id: Entity.Type,
        type: Type,
        labelFormatter: LabelFormatter = LabelFormatter.FromElement,
    ) {
        action += Action.RegisterSynthesizedEntity(id, type, labelFormatter)
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

    /**
     * Embeds the provided class into template and adds it to the set of input classes.
     * It takes a class as an argument, referred to as [cls], and embeds this class into the current
     * System Model Template.
     *
     * The inject() method is primarily used to introduce new classes to the system model that are not
     * originally part of the input classes. These could be utility classes or
     * any other classes that augment the functionality or structural understanding of the system.
     */
    fun inject(cls: KClass<*>) {
        inject(classNode(cls).toBytes())
    }

    fun inject(cls: Class<*>) {
        inject(classNode(cls).toBytes())
    }

    /**
     * Embeds the provided class into template and adds it to the set of input classes.
     * It takes a ByteArray representation of a class file as an argument,
     * referred to as [classBytes], and embeds this class into the current System Model Template.
     *
     * The inject() method is primarily used to introduce new classes to the system model that are not
     * originally part of the input classes. These could be utility classes or
     * any other classes that augment the functionality or structural understanding of the system.
     */
    fun inject(classBytes: ByteArray) {
        action += Action.InjectClass(classBytes)
    }
}