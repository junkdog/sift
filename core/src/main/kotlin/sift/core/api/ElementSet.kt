package sift.core.api

import sift.core.element.Element

internal class ElementSet {
    private var elements: IntArray = IntArray(10)
    private var nextInsertionIndex: Int = 0

    operator fun plusAssign(element: Element) {
        if (nextInsertionIndex >= elements.size) {
            elements = elements.copyOf(elements.size * 2)
        }

        elements[nextInsertionIndex++] = element.id
    }

    operator fun contains(element: Element): Boolean = contains(element.id)

    @OptIn(ExperimentalStdlibApi::class)
    operator fun contains(elementId: Int): Boolean {
        for (i in 0..<nextInsertionIndex) {
            if (elements[i] == elementId)
                return true
        }

        return false
    }
}