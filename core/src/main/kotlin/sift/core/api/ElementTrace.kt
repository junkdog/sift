package sift.core.api

import sift.core.element.*
import java.util.*
import kotlin.collections.ArrayList

internal class ElementTrace private constructor(
    val elements: List<Element>
) : Sequence<Element> {

    constructor(visited: Element) : this(listOf(visited))

    operator fun plus(element: Element): ElementTrace {
        return ElementTrace(ArrayList<Element>(elements.size + 1).also {
            it += element
            it += elements
        })
    }

    override fun iterator(): Iterator<Element> {
        return elements.iterator()
    }

    operator fun contains(other: ElementTrace): Boolean {
        return Collections.indexOfSubList(other.elements, elements) != -1
    }

    operator fun contains(element: Element): Boolean {
        return element in elements
    }

    override fun toString(): String {
        return "ScopeTrail(${joinToString(separator = " <- ") { it.simpleName }})"
    }

    override fun hashCode(): Int {
        return elements.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? ElementTrace)?.elements == elements
    }
}
