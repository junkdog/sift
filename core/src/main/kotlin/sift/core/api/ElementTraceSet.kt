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


    fun findElementPerTrace(candidates: ElementSet): List<Int> {
        return _traces
            .mapNotNull { trace -> trace.findElement(candidates) }
    }

    operator fun contains(candidateTrace: Pair<ElementTrace, Element>): Boolean {
        return _traces.any { candidateTrace in it }
    }

    operator fun contains(candidateTrace: ElementTrace): Boolean {
        return _traces.any { candidateTrace in it }
    }

    fun removeAll(remove: List<ElementTrace>) {
        _traces.removeAll { trace -> remove.any { it in trace } }
    }

    fun addAll(transitions: List<ElementTrace>) {
        val traces = _traces
        for (t in transitions) {
            var replaced = false
            for (i in traces.indices) {
//                if (traces[i] in t) {
                if (t in traces[i]) {
                    traces[i] = t
                    replaced = true
                    break
                }
            }

            if (!replaced)
                this += t
        }
//        removeAll(transitions)
//        transitions.forEach { this += it }
    }
}
