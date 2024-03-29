package sift.core.api

import net.onedaybeard.collectionsby.findBy
import sift.core.SynthesisTemplate
import sift.core.TemplateProcessingException
import sift.core.asm.classNode
import sift.core.asm.resolveClassNodes
import sift.core.element.*
import sift.core.element.AsmClassNode
import sift.core.entity.EntityService
import sift.core.tree.ElementNode
import sift.core.tree.Tree
import sift.core.tree.merge
import java.net.URI
import java.util.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class TemplateProcessor(classNodes: Iterable<AsmClassNode>) {
    internal val context: Context = Context.from(classNodes)

    fun execute(template: Action<Unit, Unit>, profile: Boolean): SystemModel {
        return process(template, profile).let(::SystemModel)
    }

    internal fun process(
        template: Action<Unit, Unit>,
        profile: Boolean,
        onComplete: (Context) -> Unit = {}
    ): Context {
        context.profiling = profile

        val start = System.nanoTime()
        try {
            template(context, Unit)
        } catch (e: Exception) {
            if (profile)
                updateMeasurements(start, System.nanoTime())

            throw TemplateProcessingException(context.measurements.takeIf { profile }, e)
        }
        context.updateEntityLabels()
        val end = System.nanoTime()
        context.stats.templateProcessing = (end - start).nanoseconds

        onComplete(context)

        if (profile)
            updateMeasurements(start, end)

        return context
    }


    private fun updateMeasurements(
        start: Long,
        end: Long
    ) {
        // tag scopes
        context.measurements
            .walk()
            .filter { "-scope" in it.value.action }
            .forEach { node ->
                enumValues<MeasurementScope>()
                    .findBy(MeasurementScope::id, node.value.action)
                    ?.let { scope -> node.value.scopeIn = scope }
            }

        // mark scopes
        context.measurements
            .walk()
            .forEach { node ->
                node.value.scopeIn = generateSequence(node, Tree<Measurement>::prev)
                    .first { it.value.scopeIn != MeasurementScope.FromContext }
                    .value
                    .scopeIn

                node.prev?.value?.scopeOut = node.value.scopeIn

                if (node.children().isNotEmpty())
                    node.value.execution = (-1).nanoseconds
            }

        context.measurements.value.let { root ->
            root.execution = (end - start).nanoseconds
            root.action = "template"
            root.entites = context.entityService.allEntities().size
        }
    }

    companion object {

        @ExperimentalTime
        fun from(
            source: String,
            mavenRepositories: List<URI>,
        ): TemplateProcessor {
            val (cns, duration) = measureTimedValue {
                resolveClassNodes(source, mavenRepositories)
            }

            return TemplateProcessor(cns)
                .also { it.context.stats.parseAsmClassNodes = duration }
        }
    }
}

fun TemplateProcessor.traceElementId(elementId: Int, reversed: Boolean): Tree<ElementNode> {
    return tracesOfElementId(elementId)
        .map { if (reversed) it.reversed().intoTree() else it.intoTree() }
        .let { trees -> elementTreeOf(trees, context.entityService) }
}

private fun TemplateProcessor.elementTreeOf(traces: List<Tree<Element>>, es: EntityService): Tree<ElementNode> {
    val root = ClassNode.from(classNode(SynthesisTemplate::class))
    val traceCount: (Element) -> Int = { if (it.id != -1) context.elementAssociations.tracesOf(it.id).size else 0 }
    return traces
        .fold(Tree<Element>(root)) { tree, trace -> merge(root, tree, trace, { a, b -> a.value.id == b.value.id }, {elem, _ -> elem }) }
        .map { elem -> ElementNode(elem, es[elem], traceCount(elem)) }
}



internal fun <T> List<T>.intoTree(): Tree<T> {
    val root = Tree(first())
    var current = root

    drop(1).forEach { element -> current = current.add(element) }
    return root
}

private fun TemplateProcessor.tracesOfElementId(elementId: Int): List<List<Element>> {
    return context.elementAssociations.tracesOf(elementId)
}

