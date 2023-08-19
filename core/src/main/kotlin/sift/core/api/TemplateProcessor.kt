package sift.core.api

import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.Opcodes
import sift.core.SynthesisTemplate
import sift.core.TemplateProcessingException
import sift.core.asm.classNode
import sift.core.asm.resolveClassNodes
import sift.core.element.*
import sift.core.element.AsmClassNode
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.tree.Tree
import sift.core.tree.merge
import java.net.URI
import java.util.*
import kotlin.Comparator
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

fun TemplateProcessor.traceElementId(elementId: Int, inverseTraces: Boolean): Tree<ElementNode> {
    return tracesOfElementId(elementId)
        .map { if (inverseTraces) it.reversed().intoTree() else it.intoTree() }
        .let { trees -> elementTreeOf(trees, context.entityService) }
}

//private fun elementTreeOf(traces: List<Tree<Element>>, es: EntityService): Tree<ElementNode> {
//    val cn = classNode(SynthesisTemplate::class) // dummy row, removed later
//    return traces
//        .fold(Tree<Element>(ClassNode.from(cn))) { tree, trace -> tree.mergeAdd(trace) }
//        .map { elem -> ElementNode(elem.toString(), elem::class.simpleName!!, elem.id, es[elem]?.type, es[elem]?.id) }
//}

private fun TemplateProcessor.elementTreeOf(traces: List<Tree<Element>>, es: EntityService): Tree<ElementNode> {
    val root = ClassNode.from(classNode(SynthesisTemplate::class))
    val traceCount: (Element) -> Int = { if (it.id != -1) context.elementAssociations.tracesOf(it.id).size else 0 }
    return traces
        .fold(Tree<Element>(root)) { tree, trace -> merge(root, tree, trace, { a, b -> a.value.id == b.value.id }, {elem, _ -> elem }) }
        .map { elem -> ElementNode(elem, es[elem], traceCount(elem)) }
}

private fun Element.properties(): List<String> {
    return when (this) {
        is AnnotationNode -> listOfNotNull()
        is ClassNode -> listOfNotNull(
            "kotlin".takeIf { isKotlin },
            "iface".takeIf { isInterface },
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
        )
        is FieldNode -> listOfNotNull(
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
            "static".takeIf { access and Opcodes.ACC_STATIC != 0 },
        )
        is MethodNode -> listOfNotNull(
            "abstract".takeIf { isAbstract },
            "static".takeIf { access and Opcodes.ACC_STATIC != 0 },
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
        )
        is ParameterNode -> listOfNotNull()
        is SignatureNode -> listOfNotNull()
        is ValueNode -> listOf()
    }
}


internal fun <T> List<T>.intoTree(): Tree<T> {
    // todo: use fold
    val root = Tree(first())
    var current = root

    drop(1).forEach { element -> current = current.add(element) }
    return root
}

private fun TemplateProcessor.tracesOfElementId(elementId: Int): List<List<Element>> {
    return context.elementAssociations.tracesOf(elementId)
}

data class ElementNode(
    val label: String,
    val type: String,
    val elementId: Int,
    val entityType: Entity.Type?,
    val entityId: UUID?,
    val traces: Int,
    val properties: List<String>
) : Comparator<ElementNode>, Comparable<ElementNode> {
    constructor(element: Element, entity: Entity?, tracesToElement: Int) : this(
        element.toString(),
        element::class.simpleName!!,
        element.id,
        entity?.type,
        entity?.id,
        tracesToElement,
        element.properties()
    )

    override fun compareTo(other: ElementNode): Int {
        return compare(this, other)
    }

    override fun toString(): String = "$elementId: $label <<${type.replace("Node", "")}>>"
    override fun compare(o1: ElementNode, o2: ElementNode): Int = o1.elementId.compareTo(o2.elementId)
}