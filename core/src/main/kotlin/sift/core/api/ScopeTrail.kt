package sift.core.api

import sift.core.element.*

internal class ScopeTrail : Sequence<Element> {
    constructor(visited: Element) : this(listOf(visited))

    private constructor(visited: List<Element>) {
        elements += visited
    }

    private val elements: MutableList<Element> = mutableListOf()

    operator fun plus(element: Element): ScopeTrail {
        return ScopeTrail(elements + element)
    }

    override fun iterator(): Iterator<Element> {
        return elements.reversed().iterator()
    }

    operator fun contains(other: ScopeTrail): Boolean {
        return other.elements.containsAll(elements)
    }

    override fun toString(): String {
        return "ScopeTrail(${joinToString(separator = " <- ") { it.simpleName }})"
    }
}
