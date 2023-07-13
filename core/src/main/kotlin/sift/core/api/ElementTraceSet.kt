package sift.core.api

import sift.core.element.Element

internal class ElementTraceSet {
    private val _traces: MutableList<ElementTrace> = mutableListOf()

    val traces: List<ElementTrace>
        get() = _traces

    val tracedElementCount: Int
        get() = _traces.sumOf(ElementTrace::size)

    operator fun plusAssign(trace: ElementTrace) {
        _traces += trace
    }

    fun removeAll(remove: List<ElementTrace>) {
        _traces.removeAll { trace -> remove.any { it in trace } } // does the above line work???
    }

    fun firstTracedElements(candidates: ElementSet): List<Int> {
        return _traces
            .mapNotNull { trace -> trace.findElement(candidates) }
    }

    operator fun contains(candidateTrace: Pair<ElementTrace, Element>): Boolean {
        return _traces.any { candidateTrace in it }
    }

    operator fun contains(candidateTrace: ElementTrace): Boolean {
        return _traces.any { candidateTrace in it }
    }

    fun addAll(transitions: List<ElementTrace>) {
        transitions.forEach { this += it }
    }
}