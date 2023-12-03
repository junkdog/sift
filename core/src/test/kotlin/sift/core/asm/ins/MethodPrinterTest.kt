package sift.core.asm.ins

import net.onedaybeard.collectionsby.firstBy
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.*
import sift.core.asm.classNode
import sift.core.element.AsmClassNode
import sift.core.element.AsmMethodNode
import sift.core.entity.EntityService
import sift.core.graphviz.DiagramGenerator
import java.util.UUID


class MethodPrinterTest {
    @Test
    fun `sandbox`() {
//        val method = "method with if"
        val method = "build"
//        val method = "register\$core"

//        val cn = classNode(TestClass::class)
//        val cn = classNode(EntityService::class)
        val cn = classNode(DiagramGenerator::class)
        cn.methods
//            .firstBy(AsmMethodNode::name, "build")
            .firstBy(AsmMethodNode::name, method)
            .instructions
            .asIterable()
            .map { ins -> renderTerminal(ins, renderInstruction(ins)) }
            .joinToString("\n")
            .let(::println)

//        flowAnalyze(cn, cn.methods.firstBy(AsmMethodNode::name, method))
//        dataflowAnalyze(cn, cn.methods.firstBy(AsmMethodNode::name, method))
//        defintionUseCase(cn, cn.methods.firstBy(AsmMethodNode::name, method))

        val analyzer = SiftAnalyzer<BasicValue>(BasicInterpreter())
        analyze(analyzer, cn, cn.methods.firstBy(AsmMethodNode::name, method))
//        analyze(cn, cn.methods.firstBy(AsmMethodNode::name, "build"))


        val graphviz = renderMethod(cn.methods.firstBy(AsmMethodNode::name, method), analyzer.linearBytecodeSegments())
        graphviz.let(::println)
    }
}

private fun analyze(
    analyzer: SiftAnalyzer<BasicValue>,
    owner: AsmClassNode,
    mn: AsmMethodNode
) {
    analyzer.analyze(owner.name, mn)
    analyzer.linearBytecodeSegments()
}

private fun analyzeTypeFlow(owner: AsmClassNode, mn: AsmMethodNode) {
//    val a: Analyzer<BasicValue> = TypeFlowAnalyzer<BasicValue>(BasicInterpreter(), mn.instructions.toList())
    val a: Analyzer<BasicValue> = TypeFlowAnalyzer<BasicValue>(BasicInterpreter(), mn.instructions.toList())
    try {
        a.analyze(owner.name, mn)
//        val frames: Array<Frame<BasicValue>?> = a.getFrames()
//        val insns = mn.instructions.toArray()
//        for (i in frames.indices) {
//            if (frames[i] == null && insns[i] !is LabelNode) {
//                mn.instructions.remove(insns[i])
//            }
//        }



        println("hi")
    } catch (e: AnalyzerException) {
        e.printStackTrace()
    }
}

fun <V : Value> Frame<V>.stack(): List<V?> {
    return (0..stackSize - 1).reversed().map(::getStack)
}


class TypeFlowAnalyzer<V : Value>(
    val interpreter: Interpreter<V>,
    val instructions: List<AbstractInsnNode>
) : Analyzer<V>(interpreter) {
//    val controlFlow: Tree<>

    val Int.simpleName
        get() = instructions[this]::class.simpleName

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        println("newControlFlowEdge($insnIndex:${insnIndex.simpleName}, ${successorIndex}:${successorIndex.simpleName})")
        super.newControlFlowEdge(insnIndex, successorIndex)
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        println("newControlFlowExceptionEdge(${insnIndex}:${insnIndex.simpleName}, ${successorIndex}:${successorIndex.simpleName})")
        return super.newControlFlowExceptionEdge(insnIndex, successorIndex)
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, tryCatchBlock: TryCatchBlockNode?): Boolean {
        println("newControlFlowExceptionEdge($insnIndex: ${insnIndex.simpleName}, $tryCatchBlock)")

        return super.newControlFlowExceptionEdge(insnIndex, tryCatchBlock)
    }
}




internal class SiftFrame<V : Value>: Frame<V> {
    constructor(numLocals: Int, maxStack: Int) : super(numLocals, maxStack)
    constructor(frame: Frame<V>) : super(frame)

    lateinit var ins: AbstractInsnNode
    var index: Int = -1
    val successors: MutableSet<SiftFrame<V>> = mutableSetOf()
    val predecessors: MutableSet<SiftFrame<V>> = mutableSetOf()

    var segment: LinearBytecodeSegment? = null

    var isExceptionPath: Boolean = false

    override fun toString(): String {
        return if (this::ins.isInitialized) {
            "$index ${ins::class.simpleName}"
        } else "$index "
    }
}

@Suppress("UNCHECKED_CAST")
internal class SiftAnalyzer<V: Value>(
    interpreter: Interpreter<V>,
) : Analyzer<V>(interpreter) {

    private var instructions: List<AbstractInsnNode> = listOf()

    override fun init(owner: String, method: MethodNode) {
        instructions = method.instructions.toList()
    }

    override fun newFrame(frame: Frame<out V>): Frame<V> {
        return SiftFrame(frame) as Frame<V>
    }

    override fun newFrame(numLocals: Int, numStack: Int): Frame<V> {
        return SiftFrame(numLocals, numStack)
    }

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        insnIndex.frame.apply {
            successors += successorIndex.frame
            successorIndex.frame.predecessors += this
            ins = insnIndex.ins
            index = insnIndex
        }
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        insnIndex.frame.apply {
            successors += successorIndex.frame
            successorIndex.frame.predecessors += this
            ins = insnIndex.ins
            index = insnIndex
            isExceptionPath = true
        }
        return true
    }

    fun linearBytecodeSegments(): List<LinearBytecodeSegment> {
        val segments: MutableList<LinearBytecodeSegment> = mutableListOf()

        val current: MutableList<SiftFrame<BasicValue>> = mutableListOf()
        for (f in frames.filter { it.index != -1 }) {
            when (f.ins) {
                is LabelNode -> {
                    if ((f.predecessors.size > 1 || f.successors.size > 1) && current.isNotEmpty()) {
                        segments += LinearBytecodeSegment(current.toList())
                            .also { current.first().segment = it }
                        current.clear()
                    }
                    current += f
                }
                is JumpInsnNode -> {
                    current += f
                    segments += LinearBytecodeSegment(current.toList())
                        .also { current.first().segment = it }
                    current.clear()
                }
                else -> {
                    current += f
                }
            }
        }

        if (current.isNotEmpty()) {
            segments += LinearBytecodeSegment(current.toList())
                .also { current.first().segment = it }
            current.clear()
        }

        return segments
    }

    private val frames: List<SiftFrame<BasicValue>>
        get() = getFrames().filterNotNull() as List<SiftFrame<BasicValue>>

    private val Int.frame: SiftFrame<BasicValue>
        get() = getFrames()[this] as SiftFrame<BasicValue>

    private val Int.ins: AbstractInsnNode
        get() = instructions[this]
}

internal data class LinearBytecodeSegment(
    val instructions: List<SiftFrame<BasicValue>>
) {
    val id = UUID.randomUUID()

    fun successors(): List<LinearBytecodeSegment> {
        return instructions
            .last()
            .successors
            .mapNotNull(SiftFrame<BasicValue>::segment)
    }

    fun predecessor(): List<LinearBytecodeSegment> {
        return instructions
            .first()
            .predecessors
            .mapNotNull(SiftFrame<BasicValue>::segment)
    }
}

class TestClass {
    fun `method with no branches`(a: Int, b: Int) {
        val c = a + b
        println(c)
    }

fun `method with if`(a: Int, b: Int) {
    val c = if (a > b) a else b
    println(c)
}

    fun `method with return statement`(a: Int, b: Int): Int {
        val c = if (a > b) a else b
        return c
    }
}