package sift.core.dsl

import sift.core.api.*
import sift.core.api.chainFrom
import sift.core.element.AnnotationNode
import sift.core.entity.Entity

/**
 * Annotations scope.
 */
@SiftTemplateDsl
class Annotations internal constructor(
    annotations: Action<Iter<AnnotationNode>, Iter<AnnotationNode>> = Action.Annotations.AnnotationScope,
    action: Action.Chain<IterAnnotations> = chainFrom(annotations),
) : Core<AnnotationNode>(action),
    CommonOperations<AnnotationNode, Annotations>
{
    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Annotations.Filter(regex, invert)
    }

    /** Iterates over child annotations referenced by the specified [element]. */
    fun nested(element: String, f: Annotations.() -> Unit) {
        val forkTo = Action.Annotations.NestedAnnotations(element)
            .andThen(Annotations().also(f).action)

        action += Action.Fork(forkTo)
    }

    /**
     * Iterates over classes stored within a specified annotation [element]. If [synthesize] is set to `true`,
     * missing types are stubbed based on those stored within the annotation element.
     */
    fun explodeTypes(
        element: String,
        synthesize: Boolean = false,
        f: Classes.() -> Unit
    ) {
        val forkTo = Action.Annotations.ExplodeType(element, synthesize)
            .andThen(Classes().also(f).action)

        action += Action.Fork(forkTo)
    }

    override fun scope(label: String, f: Annotations.() -> Unit) {
        action += Action.Fork(label.takeIf(String::isNotEmpty), Annotations().also(f).action)
    }

    override fun scope(
        label: String,
        op: ScopeEntityPredicate,
        entity: Entity.Type,
        f: Annotations.() -> Unit
    ) {
        val forkTo = Annotations().also(f).action
        action += Action.ForkOnEntityExistence(forkTo, entity, op == ScopeEntityPredicate.ifExistsNot)
    }
}
