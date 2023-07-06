package sift.core.api

import sift.core.element.AnnotationNode
import sift.core.element.Element
import sift.core.element.ValueNode
import sift.core.entity.Entity
import sift.core.entity.EntityService

internal class ElementAssociationRegistry(
    val entityService: EntityService
) {
    private val traces: MutableList<MutableList<ElementTrace>> = mutableListOf()
    private val tracedElements: MutableList<Element> = mutableListOf()

    // entities registered to elements
    private val registeredElements: MutableMap<Entity.Type, MutableSet<Int>> = mutableMapOf()

    fun allTraces(): List<List<Element>> = traces.flatten()
        .map { trace -> trace.map { id -> tracedElements[id] } }

    @Suppress("NAME_SHADOWING")
    fun registerTransition(from: Element, to: Element) {
        val from = sanitized(from)
        val to = sanitized(to)

        if (from == to)
            return

        // existing traces of element being scoped to
        val currentTraces = tracesOf(to)

        // resolve new traces; filter already scoped element traces
        val transitions: List<ElementTrace> = tracesOf(from)
            .map { it + to }
            .filter { it !in currentTraces }

        currentTraces
            .also { trails -> trails.removeAll { o -> transitions.any { it in o } } }
            .addAll(transitions)
    }

    fun register(entity: Entity, element: Element): Element {
        val sanitized = sanitized(element)
        registeredElements.getOrPut(entity.type) { mutableSetOf() }
            .add(sanitized.id)

        return sanitized
    }

    fun findRelatedEntities(input: Element, entity: Entity.Type): Set<Entity> {
        val tracedElement = sanitized(input)

        // the most immediate path back to the root element
        val candidateElements = registeredElements[entity]
        val plain = when {
            candidateElements != null -> tracesOf(tracedElement)
                .mapNotNull { entityService.filter(it, candidateElements) }
                .toSet()

            else -> emptySet()
        }

        // check if input element is contained in the trails of eligible entities
        val reverse = entityService[entity]
            .map { (elem, e) -> e to tracesOf(elem) }
            .asSequence()
            .flatMap { (e, trails) -> trails.map { e to it } }
            .filter { (_, trail) -> tracedElement in trail }
            .map { (e, _) -> e }
            .toSet()

        return plain + reverse
    }

    fun statistics(): Map<String, Int> = mapOf(
        "associations.keys"              to traces.size,
        "associations.traces"            to traces.flatten().size,
        "associations.traces.p50"        to traces.p(50, List<ElementTrace>::size),
        "associations.traces.p90"        to traces.p(90, List<ElementTrace>::size),
        "associations.traces.max"        to traces.maxOf(List<ElementTrace>::size),
        "associations.traces.depth.p50"  to traces.flatten().p(50) { it.asIterable().count() },
        "associations.traces.depth.p90"  to traces.flatten().p(90) { it.asIterable().count() },
        "associations.traces.depth.max"  to traces.flatten().maxOf { it.asIterable().count() },
        "associations.flatten"           to traces.flatten().sumOf { it.asIterable().count() },
    )

    private fun sanitized(element: Element): Element {
        val traceable = resolveTracedElement(element)
        if (traceable.id == -1) {
            // todo: thread-safety/assert single-threaded access
            traceable.id = tracedElements.size
            tracedElements += traceable
            traces += mutableListOf(mutableListOf(ElementTrace(traceable)))
        }

        return traceable
    }

    private fun tracesOf(element: Element): MutableList<ElementTrace> = traces[element.id]

    private fun EntityService.filter(
        trail: ElementTrace,
        candidateElements: Set<Int>
    ): Entity? = trail
        .filter { it in candidateElements }
        .map { this[tracedElements[it]] }
        .firstOrNull() // TODO: verify that this can't be multiple entities<?>
}

private fun resolveTracedElement(input: Element): Element {
    // to avoid excessive resource consumption, we avoid bookkeeping
    // traces of elements that are intrinsically tied to other (traced) elements.
    return (input as? ValueNode)?.reference?.let(::resolveTracedElement)
        ?: (input as? AnnotationNode)?.root
        ?: input
}

private inline fun <T> Iterable<T>.p(
    percentile: Int,
    selector: (T) -> Int
): Int {
    val elements = map(selector).sorted()
    return elements[(elements.size * percentile * 0.01).toInt()]
}