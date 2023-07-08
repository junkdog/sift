package sift.core.api

import sift.core.element.Element

internal class ElementTrace private constructor(
    private val elements: IntArray,
    private val bloomHash: ULong
) : Iterable<Int> {

    constructor(visited: Element) : this(intArrayOf(visited.id), visited.id.bloomBit)

    operator fun plus(element: Element): ElementTrace {
        val ids = IntArray(elements.size + 1).also { dest ->
            dest[0] = element.id
            elements.copyInto(dest, 1)
        }

        return ElementTrace(ids, bloomHash or element.id.bloomBit)
    }

    override fun iterator(): Iterator<Int> = elements.iterator()
    operator fun contains(other: ElementTrace): Boolean {
        return (bloomHash and other.bloomHash) == bloomHash
            && elements.all { it in other.elements }
    }
    operator fun contains(element: Element): Boolean {
        return element.id.bloomBit and bloomHash != 0uL
            && element.id in elements
    }

    override fun toString(): String = "ElementTrace(${elements.joinToString(separator = " < ") { it.toString() }})"
    override fun hashCode(): Int = elements.contentHashCode()
    override fun equals(other: Any?): Boolean {
        return other is ElementTrace
            && other.bloomHash == bloomHash
            && other.elements contentEquals elements
    }

    /** finds the first matching element id in this trace */
    fun findElement(candidates: ElementSet): Int? {
        if (candidates.bloomHash and bloomHash == 0uL)
            return null

        val elems = elements
        for (i in 0..elems.lastIndex) {
            if (elems[i] in candidates) {
                return elems[i]
            }
        }

        return null
    }
}
