package sift.core.api

import sift.core.element.Element

/**
 * The ElementTraceSet class is responsible for managing and operating on the traces elements visited by the DSL.
 * Each instance of this class is associated with an [Element], storing the [ElementTrace]:s leading up to that
 * element. These traces are used for relating entities and updating entity properties.
 *
 * @see ElementTraceRegistry.traces
 */
internal class ElementTraceSet {
    private val _traces: MutableList<ElementTrace> = mutableListOf()

    val traces: List<ElementTrace>
        get() = _traces

    operator fun plusAssign(trace: ElementTrace) {
        _traces += trace
    }

    /**
     * Retrieves the first matching element from each [ElementTrace] contained in this set.
     *
     * @param candidates The set of candidate elements to match against.
     * @return id:s of the first matching element from each `ElementTrace`.
     */
    fun findElementPerTrace(candidates: ElementSet): List<Int> {
        return _traces
            .mapNotNull { trace -> trace.findElement(candidates) }
    }

    operator fun contains(candidateTrace: Pair<ElementTrace, Element>): Boolean {
        return _traces.any { candidateTrace in it }
    }

    fun addAll(transitions: List<ElementTrace>) {
        val traces = _traces

        for (t in transitions) {
            var i = 0
            while (i < traces.size) {
                if (traces[i] in t) {
                    traces.swap(i, traces.lastIndex)
                    traces.removeLast()
                } else {
                    i++
                }
            }
        }

        transitions.forEach { this += it }
    }
}

private fun <E> MutableList<E>.swap(a: Int, b: Int) {
    val oldA = this[a]
    this[a] = this[b]
    this[b] = oldA
}
