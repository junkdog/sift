package sift.core.api

import sift.core.element.Element

internal class ElementSet {
    private var elements: IntArray = IntArray(10)
    private var nextIndex: Int = 0
    var bloomHash: ULong = 0uL
        private set

    operator fun plusAssign(element: Element) {
        if (nextIndex >= elements.size) {
            elements = elements.copyOf(elements.size * 3 / 2)
        }

        bloomHash = bloomHash or element.id.bloomBit
        elements[nextIndex++] = element.id
    }

    operator fun contains(element: Element): Boolean = contains(element.id)

    @OptIn(ExperimentalStdlibApi::class)
    operator fun contains(elementId: Int): Boolean {
        if (elementId.bloomBit and bloomHash == 0uL)
            return false

        for (i in 0..<nextIndex) {
            if (elements[i] == elementId)
                return true
        }

        return false
    }
}

internal val Int.bloomBit get() = 1uL shl (this % 64)