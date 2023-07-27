package sift.core.api

import sift.core.element.Element

internal class ElementTrace private constructor(
    internal val elements: IntArray,
    internal val bloomMask: ULong
) {

    constructor(visited: Element) : this(intArrayOf(visited.id), visited.id.bloomBit)

    operator fun plus(element: Element): ElementTrace {
        val ids = IntArray(elements.size + 1).also { dest ->
            dest[0] = element.id
            elements.copyInto(dest, 1)
        }

        return ElementTrace(ids, bloomMask or element.id.bloomBit)
    }

    operator fun contains(other: Pair<ElementTrace, Element>): Boolean {
        val (trace, element) = other
        val mask = trace.bloomMask or element.id.bloomBit
        return (bloomMask and mask) == mask
            && trace.elements.all { it in elements }
//            && trace.elements.isSubArrayOf(elements)
            && element.id in elements
    }

    operator fun contains(other: ElementTrace): Boolean {
        return (bloomMask and other.bloomMask) == other.bloomMask
            && other.elements.all { it in elements }
//            && other.elements.isSubArrayOf(elements)
    }

    operator fun contains(element: Element): Boolean {
        return element.id.bloomBit and bloomMask != 0uL
            && element.id in elements
    }

    override fun toString(): String = "ElementTrace(${elements.joinToString { it.toString() }})"
    override fun hashCode(): Int = elements.contentHashCode()
    override fun equals(other: Any?): Boolean {
        return other is ElementTrace
            && other.bloomMask == bloomMask
            && other.elements contentEquals elements
    }

    /** finds the first matching element id in this trace */
    fun findElement(candidates: ElementSet): Int? {
        if (candidates.bloomMask and bloomMask == 0uL)
            return null

        val elems = elements
        for (i in 0..elems.lastIndex) {
            if (elems[i] in candidates) {
                return elems[i]
            }
        }

        return null
    }

    fun findElements(candidates: ElementSet): List<Int> {
        if (candidates.bloomMask and bloomMask == 0uL)
            return listOf()

        val found = mutableListOf<Int>()

        val elems = elements
        for (i in 0..elems.lastIndex) {
            if (elems[i] in candidates) {
                found += elems[i]
//                return elems[i]
            }
        }

        return found
    }

    val size: Int
        get() = elements.size
}



fun IntArray.isSubArrayOf(rhs: IntArray): Boolean {
    var i = 0
    var j = 0

    while (i < size && j < rhs.size) {
        if (this[i] == rhs[j]) {
            i++
        }
        j++
    }

    // If we've found all elements of 'a' in 'b', then i will have advanced to a.size
    return i == size
}