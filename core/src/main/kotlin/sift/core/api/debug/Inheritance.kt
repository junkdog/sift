package sift.core.api.debug

import sift.core.SynthesisTemplate
import sift.core.api.Context
import sift.core.api.TypeClassNode
import sift.core.asm.classNode
import sift.core.dsl.type
import sift.core.element.ClassNode
import sift.core.element.MethodNode
import sift.core.terminal.Gruvbox.dark2
import sift.core.terminal.Gruvbox.dark3
import sift.core.terminal.Gruvbox.fg
import sift.core.terminal.Gruvbox.orange2
import sift.core.tree.Tree

internal fun Context.debugInheritanceTrees(): String {
    val tree = Tree(TypeClassNode(type(SynthesisTemplate::class), ClassNode.from(classNode(SynthesisTemplate::class))))
    inheritance.values.forEach(tree::add)

    val idLength = tree.walk().drop(1).maxOf { it.value.elementId.length }
    val etLength = tree.walk().drop(1).maxOf { it.value.properties.length }
    val genericsLength = tree.walk().drop(1).maxOf { it.value.formalTypeParameters.length }

    val formattedTree = tree.toString(
        format = TypeClassNode::toDebugString,
        prefix = { node ->
            listOf(
                // element id
                dark3(node.elementId.padStart(idLength)),
                // element type
                dark2(node.properties.padEnd(etLength)),
                // formal type parameters
                orange2(node.formalTypeParameters.padEnd(genericsLength)),
            ).joinToString(" ")
        })

    return formattedTree.lines().drop(1).joinToString("\n")
}

@Suppress("SimplifiableCallChain") // transforming joinToString is uglier to read
private fun TypeClassNode.toDebugString(): String {
    val className = toString()
    val methods = (cn ?: return fg(className))
        .methods
        .map(MethodNode::toDebugString)
        .joinToString(" ")

    return "${fg(className)} ${dark3(methods)}"
}

private fun MethodNode.toDebugString(): String =
    "$name(${parameters.joinToString { "${it.name}: ${it.type.simpleName}" }})"

private val TypeClassNode.properties: String
    get() = listOfNotNull(
        "anno".takeIf { cn?.isAnnotation == true },
        "iface".takeIf { isInterface && cn?.isAnnotation != true },
        "class".takeIf { !isInterface && cn?.isEnum != true },
        "enum".takeIf { cn?.isEnum == true },
        "undef".takeIf { cn == null },
    ).joinToString(" ")

private val TypeClassNode.elementId: String
    get() = cn?.id?.takeIf { it != -1 }?.toString() ?: ""

private val TypeClassNode.formalTypeParameters: String
    get() = (cn?.signature?.formalParameters ?: listOf()).joinToString(", ")

