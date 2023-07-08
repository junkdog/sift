package sift.core.api

import sift.core.element.Element

internal class ElementTrace private constructor(
    private val elements: IntArray,
) : Iterable<Int> {
    private val hash = elements.contentHashCode()

    constructor(visited: Element) : this(intArrayOf(visited.id))

    operator fun plus(element: Element): ElementTrace {
        return ElementTrace(IntArray(elements.size + 1).also { dest ->
            dest[0] = element.id
            elements.copyInto(dest, 1)
        })
    }

    override fun iterator(): Iterator<Int> = elements.iterator()
    operator fun contains(other: ElementTrace): Boolean = elements.all { it in other.elements }
    operator fun contains(element: Element): Boolean = element.id in elements

    override fun toString(): String = "ElementTrace(${elements.joinToString(separator = " < ") { it.toString() }})"
    override fun hashCode(): Int = hash
    override fun equals(other: Any?): Boolean {
        return other is ElementTrace
            && other.hash == hash
            && other.elements contentEquals elements
    }

    /** finds the first matching element id in this trace */
    fun findElement(candidates: ElementSet): Int? {
        val elems = elements
        for (i in 0..elems.lastIndex) {
            if (elems[i] in candidates) {
                return elems[i]
            }
        }

        return null
    }
}

class SubsetIntIterator(
    private val elements: IntArray,
    private val lastIndex: Int,
) : IntIterator() {
    private var nextIndex = 0

    override fun hasNext(): Boolean = lastIndex >= nextIndex
    override fun nextInt(): Int = elements[nextIndex++]
}