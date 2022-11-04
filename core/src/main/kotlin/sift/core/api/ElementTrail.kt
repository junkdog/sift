package sift.core.api

class ElementTrail : Sequence<Element> {
    constructor(input: Element) : this(listOf(input))

    private constructor(input: List<Element>) {
        elements += input
    }

    private val elements: MutableList<Element> = mutableListOf()

    operator fun plus(element: Element): ElementTrail {
        return ElementTrail(elements + element)
    }

    override fun iterator(): Iterator<Element> {
        return elements.reversed().iterator()
    }

    override fun toString(): String {
        return "ElementTrail(${joinToString(separator = " <- ") { it.simpleName }})"
    }
}