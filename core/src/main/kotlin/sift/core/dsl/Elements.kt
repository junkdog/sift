package sift.core.dsl

import sift.core.api.*
import sift.core.api.chainFrom
import sift.core.element.Element
import sift.core.entity.Entity

/**
 * Type-erased scope exposing common functionality.
 *
 * @see [Template.elementsOf]
 */
@SiftTemplateDsl
class Elements internal constructor(
    elements: Action<Iter<Element>, Iter<Element>> = Action.Elements.ElementScope
) : Core<Element>(chainFrom(elements), AccessFlags.Scope.TypeErased),
    CommonOperations<Element, Elements>
{

    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Elements.Filter(regex, invert)
    }

    override fun scope(
        label: String,
        f: Elements.() -> Unit
    ) {
        val forkTo = Elements().also(f).action
        action += Action.Fork(label.takeIf(String::isNotEmpty), forkTo)
    }

    override fun scope(
        label: String,
        op: ScopeEntityPredicate,
        entity: Entity.Type,
        f: Elements.() -> Unit
    ) {
        val forkTo = Elements().also(f).action
        action += Action.ForkOnEntityExistence(forkTo, entity, op == ScopeEntityPredicate.ifExistsNot)
    }
}