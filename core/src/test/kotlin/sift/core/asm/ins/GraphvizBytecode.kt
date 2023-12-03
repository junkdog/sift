package sift.core.asm.ins

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.analysis.BasicValue
import sift.core.element.AsmMethodNode
import sift.core.graphviz.*
import sift.core.terminal.Gruvbox
import sift.core.terminal.Gruvbox.fg
import sift.core.terminal.TextTransformer

internal fun renderMethod(
    mn: AsmMethodNode,
    segments: List<LinearBytecodeSegment>
): String {
    val sequentialIds = TextTransformer.uuidSequence()
    val edges = segments
        .flatMap { segment -> segment.successors().map { segment to it } }
        .map { (from, to) -> nodeOf(from, sequentialIds) to nodeOf(to, sequentialIds) }
        .map { (from, to) -> "$from -> $to" }

    return """
        |digraph {
        |    // setup
        |    graph [rankdir=TB, truecolor=true, bgcolor="black", margin=0.2, nodesep=0.2, ranksep=0.2];
        |    node [
        |        shape=plain;
        |        fontname="Courier New";
        |        fontcolor="#ebdbb2";
        |        fontsize=11;
        |    ];
        |    edge [
        |        arrowhead=normal;
        |        arrowtail=dot;
        |        fontcolor="#ebdbb2";
        |        color="${fg.hexColor}";
        |        fontname="verdana";
        |        fontsize=11;
        |    ];
        |
        |    // nodes
        |    ${nodes(segments, sequentialIds)}
        |    
        |    // edges
        |    ${edges.joinToString("\n    ")}
        |    
        |}
    """.trimMargin()
}

private fun nodeOf(
    ins: LinearBytecodeSegment,
    idShortener: TextTransformer
) = "SEGMENT_${idShortener(ins.id)}"

private fun nodes(
    entities: List<LinearBytecodeSegment>,
    idShortener: TextTransformer
): String {

    fun describe(e: LinearBytecodeSegment): String {
        return """
            |${nodeOf(e, idShortener)}[label=<<table font="Courier New Bold" border="1" color="${Gruvbox.dark3.hexColor}" cellborder="0" cellspacing="0">
            |        ${tableRowsOf(e)}                
            |     </table>>];
        """.trimMargin()
    }
    return entities
        .filter { it.instructions.filter { it.ins !is LineNumberNode }.size > 1 }
        .map(::describe).joinToString("\n    ")
}

private fun tableRowsOf(block: LinearBytecodeSegment): String {
    val prefix = "        "
    return block.instructions
        .map(SiftFrame<BasicValue>::ins)
        .map(AbstractInsnNode::asTableRow)
        .joinToString("\n|$prefix")
}

internal fun AbstractInsnNode.asTableRow(): String {
    val trimmed = renderInstruction(this).trim()

    val type = trimmed.substringBefore(" ")

    val args = (trimmed.substringAfter(" ", "")
        .takeUnless(String::isEmpty) ?: "&nbsp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    val style = labelStyle

    return (if (this is LabelNode) {
        listOf(
            """<td bgcolor="${style.hexColor}" align="left" colspan="2"><font color="black">$type</font></td>""",
            """<td align="left"><font color="${argStyle.hexColor}">$args</font></td>""",
        )
    } else if (style.inverse == true) {
        listOf(
            """<td bgcolor="${style.hexColor}" width="30">&nbsp;</td>""",
            """<td bgcolor="${style.hexColor}" align="left"><font color="black">$type</font></td>""",
            """<td align="left"><font color="${argStyle.hexColor}">$args</font></td>""",
        )
    } else {
        listOf(
            """<td width="30">&nbsp;</td>""",
            """<td align="left"><font color="${style.hexColor}">$type</font></td>""",
            """<td align="left"><font color="${argStyle.hexColor}">$args</font></td>""",
        )
    }).joinToString("", prefix = "<tr>", postfix = "</tr>")
}


//fun Li