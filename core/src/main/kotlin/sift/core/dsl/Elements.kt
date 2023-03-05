package sift.core.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.api.chainFrom
import sift.core.element.Element
import sift.core.entity.Entity

class Elements internal constructor(
    elements: Action<Iter<Element>, Iter<Element>> = Action.Elements.ElementScope
) : Core<Element>(), CommonOperations<Element, Elements> {
    override var action: Action.Chain<Iter<Element>> = chainFrom(elements)

    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Elements.Filter(regex, invert)
    }

    override fun scope(label: String, op: ScopeEntityPredicate, entity: Entity.Type, f: Elements.() -> Unit) {
        TODO("Not yet implemented")
    }

    override fun scope(label: String, f: Elements.() -> Unit) {
        TODO("Not yet implemented")
    }
}