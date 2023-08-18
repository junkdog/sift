package sift.core.api

import sift.core.element.AnnotationNode
import sift.core.element.Element
import sift.core.element.ValueNode
import sift.core.entity.Entity
import sift.core.entity.EntityService

internal class ElementTraceRegistry(
    val entityService: EntityService
) {
    private val traces: MutableList<ElementTraceSet> = mutableListOf()
    val tracedElements: MutableList<Element> = mutableListOf()

    // entities registered to elements
    private val entityElements: MutableMap<Entity.Type, ElementSet> = mutableMapOf()

    fun allTraces(): List<List<Element>> = traces
        .flatMap(ElementTraceSet::traces)
        .map { trace -> trace.elements.map { id -> tracedElements[id] } }

    fun tracesOf(elementId: Int): List<List<Element>> {
        return exhaustingTracesOf(elementId)
//        return traces[elementId].traces
//            .map { trace -> trace.elements.map { id -> tracedElements[id] } }
    }

    fun exhaustingTracesOf(elementId: Int): List<List<Element>> {
        val elem =  tracedElements[elementId]
        return traces
            .flatMap { it.traces }
            .filter { elem in it }
            .map { trace -> trace.elements.map { id -> tracedElements[id] } }
    }

    @Suppress("NAME_SHADOWING")
    fun registerTransition(from: Element, to: Element) {
        val from = sanitized(from)
        val to = sanitized(to)

        if (from == to)
            return

        // existing traces of element being scoped to
        val currentTraces = tracesOf(to)

        // resolve new traces; filter already scoped element traces
        val transitions: List<ElementTrace> = tracesOf(from).traces
            .filter { (it to to) !in currentTraces }
            .map { it + to }

        currentTraces.addAll(transitions)
    }

    fun register(entity: Entity, element: Element): Element {
        return sanitized(element)
            .also { entityElements.getOrPut(entity.type, ::ElementSet) += it }
    }

    fun findRelatedEntities(input: Element, entity: Entity.Type): Set<Entity> {
        val tracedElement = sanitized(input)

        // the most immediate path back to the root element
        val candidateElements = entityElements[entity]
        val plain = when {
            candidateElements != null -> tracesOf(tracedElement)
                .findElementsPerTrace(candidateElements)
//                .findElementPerTrace(candidateElements)
                .map { tracedElements[it] }
                .mapNotNull { entityService[it] }
                .toSet()

            else -> emptySet()
        }

        // check if input element is contained in the traces of eligible entities
        val reverse = entityService[entity]
            .map { (elem, e) -> e to tracesOf(elem).traces }
            .flatMap { (e, traces) -> traces.map { e to it } }
            .filter { (_, trace) -> tracedElement in trace }
            .map { (e, _) -> e }
            .toSet()

        return plain + reverse
    }

    // FIXME: updaate trace statistics and labels
    fun statistics(): Map<String, Int> = mapOf(
        "traced-elements"  to traces.size,
        "traces"           to traces.sumOf { it.traces.size },
        "traces.p50"       to traces.map(ElementTraceSet::traces).p(50, List<ElementTrace>::size),
        "traces.p90"       to traces.map(ElementTraceSet::traces).p(90, List<ElementTrace>::size),
        "traces.p95"       to traces.map(ElementTraceSet::traces).p(95, List<ElementTrace>::size),
        "traces.p99"       to traces.map(ElementTraceSet::traces).p(99, List<ElementTrace>::size),
        "traces.max"       to (traces.maxOfOrNull { it.traces.size } ?: 0),
        "traces.depth.p50" to traces.flatMap(ElementTraceSet::traces).p(50, ElementTrace::size),
        "traces.depth.p90" to traces.flatMap(ElementTraceSet::traces).p(90, ElementTrace::size),
        "traces.depth.p95" to traces.flatMap(ElementTraceSet::traces).p(95, ElementTrace::size),
        "traces.depth.p99" to traces.flatMap(ElementTraceSet::traces).p(99, ElementTrace::size),
        "traces.depth.max" to (traces.flatMap(ElementTraceSet::traces).maxOfOrNull(ElementTrace::size) ?: 0),
        "traces.flatten"   to traces.flatMap(ElementTraceSet::traces).sumOf(ElementTrace::size),
    )

    private fun sanitized(element: Element): Element {
        val traceable = resolveTracedElement(element)
        if (traceable.id == -1) {
            traceable.id = tracedElements.size
            tracedElements += traceable
            traces += ElementTraceSet().also { it += ElementTrace(traceable) }
        }

        return traceable
    }

    private fun tracesOf(element: Element): ElementTraceSet = traces[element.id]
}

private fun resolveTracedElement(input: Element): Element {
    // to avoid excessive resource consumption, we avoid bookkeeping
    // traces of elements that are intrinsically tied to other (traced) elements.
    return (input as? ValueNode)?.reference?.let(::resolveTracedElement)
        ?: (input as? AnnotationNode)?.root
        ?: input
}

private inline fun <T> List<T>.p(
    percentile: Int,
    selector: (T) -> Int
): Int {
    if (isEmpty())
        return 0

    val elements = map(selector).sorted()
    return elements[(elements.size * percentile * 0.01).toInt()]
}