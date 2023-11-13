package sift.core.api.debug

import com.github.ajalt.mordant.rendering.TextStyles
import sift.core.SynthesisTemplate
import sift.core.api.Context
import sift.core.api.TemplateProcessor
import sift.core.asm.classNode
import sift.core.element.ClassNode
import sift.core.element.Element
import sift.core.terminal.Gruvbox
import sift.core.tree.ElementNode
import sift.core.tree.Tree
import sift.core.tree.merge

fun TemplateProcessor.elementTraceTreeOf(elementIds: List<Int>): String {
    return context.elementTraceTreeOf(elementIds)
}

internal fun Context.elementTraceTreeOf(elementIds: List<Int>): String {
    val tree = elementIds
        .map { id -> tracesOfElementId(id, true) }
        .reduce { a, b -> merge(a, b, { l, r -> l.value.elementId == r.value.elementId }) }

    val etLength = tree.walk().maxOf { it.value.entityType?.toString()?.length ?: 0 }
    val idLength = tree.walk().maxOf { it.value.elementId.toString().length }
    val propLength = tree.walk().maxOf { it.value.properties.joinToString(" ").length }
    val genericsLength = tree.walk().maxOf { it.value.formalTypeParameters.joinToString(", ").length }

    val formattedTree = tree.toString(
        format = { node ->
             when (node.type) {
                 "ClassNode"      -> Gruvbox.aqua2
                 "MethodNode"     -> Gruvbox.green2
                 "FieldNode"      -> Gruvbox.blue2
                 "ParameterNode"  -> Gruvbox.purple2
                 "SignatureNode"  -> Gruvbox.orange2
                 "AnnotationNode" -> Gruvbox.aqua1 + TextStyles.bold
                 else -> error("Unknown type: ${node.type}")
             }(node.label)
        },
        prefix = { node ->
            val eId = if (node.elementId in elementIds) Gruvbox.light3 + TextStyles.bold else Gruvbox.dark3
            listOf(
                // element id
                eId(node.elementId.toString().padStart(idLength)),
                // traces to element
                Gruvbox.dark4(node.traces.toString().padStart(3)),
                // element type
                Gruvbox.dark2(node.type.replace("Node", "").lowercase().padEnd(10)),
                // entity type
                Gruvbox.aqua1((node.entityType?.toString() ?: "").padEnd(etLength)),
                // element properties
                Gruvbox.purple1(node.properties.joinToString(" ").padEnd(propLength)),
                // formal type parameters
                Gruvbox.orange2(node.formalTypeParameters.joinToString(", ").padEnd(genericsLength)),
            ).joinToString(" ")
        })

    return when {
        "SynthesisTemplate" in formattedTree -> formattedTree.lines().drop(1).joinToString("\n")
        else -> formattedTree
    }
}

internal fun Context.debugTraces() {
    classByType.values
        .filter { it.id != -1 }
        .map(ClassNode::id)
        .let { ids -> elementTraceTreeOf(ids) }
        .let(::println)
}

internal fun Context.tracesOfElementId(elementId: Int, reversed: Boolean = true): Tree<ElementNode> {
    return elementAssociations.tracesOf(elementId)
        .map { if (reversed) it.reversed().intoTree() else it.intoTree() }
        .let { trees -> elementTraceTreeOf(trees) }
}

private fun Context.elementTraceTreeOf(traces: List<Tree<Element>>): Tree<ElementNode> {
    val root = ClassNode.from(classNode(SynthesisTemplate::class))
    val traceCount: (Element) -> Int = { if (it.id != -1) elementAssociations.tracesOf(it.id).size else 0 }
    return traces
        .fold(Tree<Element>(root)) { tree, trace ->
            merge(root, tree, trace, nodeEquals = { a, b -> a.value.id == b.value.id } )
        }
        .map { elem -> ElementNode(elem, entityService[elem], traceCount(elem)) }
}

internal fun <T> List<T>.intoTree(): Tree<T> {
    val root = Tree(first())
    var current = root

    drop(1).forEach { element -> current = current.add(element) }
    return root
}