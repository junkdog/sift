package sift.core.api

import sift.core.element.Element

internal class ElementTrace private constructor(
    private val elements: List<Element>
) : Iterable<Element> {
    private val hash = elements.hashCode()

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
        return other.elements.containsAll(elements)
    }

    operator fun contains(element: Element): Boolean {
        return element in elements
    }

    override fun toString(): String {
        return "ElementTrace(${joinToString(separator = " <- ") { it.simpleName }})"
    }

    override fun hashCode(): Int = hash

    override fun equals(other: Any?): Boolean {
        return other is ElementTrace
            && other.hash == hash
            && other.elements == elements
    }
}
