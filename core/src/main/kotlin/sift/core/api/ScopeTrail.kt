package sift.core.api

import sift.core.element.*

internal class ScopeTrail : Sequence<Element> {
    constructor(visited: Element) : this(listOf(visited))

    private constructor(visited: List<Element>) {
        elements += visited
    }

    private val elements: MutableList<Element> = mutableListOf()

    fun intersects(other: ScopeTrail): Boolean {
        return elements.intersect(other.elements.toSet()).isNotEmpty()
    }

    operator fun plus(element: Element): ScopeTrail {
        return ScopeTrail(elements + element)
    }

    override fun iterator(): Iterator<Element> {
        return elements.reversed().iterator()
    }

    operator fun contains(other: ScopeTrail): Boolean {
        return other.elements.containsAll(elements)
    }

    operator fun contains(element: Element): Boolean {
        return element in elements
    }

    override fun toString(): String {
        return "ScopeTrail(${joinToString(separator = " <- ") { it.simpleName }})"
    }
}
