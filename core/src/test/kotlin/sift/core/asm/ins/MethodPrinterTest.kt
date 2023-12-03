package sift.core.asm.ins

import net.onedaybeard.collectionsby.firstBy
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.*
import sift.core.asm.classNode
import sift.core.element.AsmClassNode
import sift.core.element.AsmMethodNode


class MethodPrinterTest {
    @Test
    fun `sandbox`() {
        val method = "method with if"

        val cn = classNode(TestClass::class)
//        val cn = classNode(EntityService::class)
//        val cn = classNode(DiagramGenerator::class)
        cn.methods
//            .firstBy(AsmMethodNode::name, "build")
            .firstBy(AsmMethodNode::name, method)
            .instructions
            .asIterable()
            .map(::render)
            .joinToString("\n")
            .let(::println)

//        flowAnalyze(cn, cn.methods.firstBy(AsmMethodNode::name, method))
//        dataflowAnalyze(cn, cn.methods.firstBy(AsmMethodNode::name, method))
//        defintionUseCase(cn, cn.methods.firstBy(AsmMethodNode::name, method))

        analyze(cn, cn.methods.firstBy(AsmMethodNode::name, method))
//        analyze(cn, cn.methods.firstBy(AsmMethodNode::name, "build"))

    }
}

private fun analyze(owner: AsmClassNode, mn: AsmMethodNode) {
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




class SiftFrame<V : Value>: Frame<V> {
    constructor(numLocals: Int, maxStack: Int) : super(numLocals, maxStack)
    constructor(frame: Frame<V>) : super(frame)

    val successors: MutableSet<Frame<V>> = mutableSetOf()
}

@Suppress("UNCHECKED_CAST")
class SiftAnalyzer<V: Value>(
    interpreter: Interpreter<V>
) : Analyzer<V>(interpreter) {
    override fun newFrame(frame: Frame<out V>): Frame<V> {
        return SiftFrame(frame) as Frame<V>
    }

    override fun newFrame(numLocals: Int, numStack: Int): Frame<V> {
        return SiftFrame(numLocals, numStack)
    }

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        insnIndex.frame.successors += successorIndex.frame
    }

    private val Int.frame: SiftFrame<V>
        get() = frames[this] as SiftFrame<V>

    private val Int.ins: SiftFrame<V>
        get() = ins
}

data class LinearBytecodeSegment(
    val instructions: List<AbstractInsnNode>
) {
    fun successors(): List<LinearBytecodeSegment> {
        TODO()
    }

    fun predecessor(): List<LinearBytecodeSegment> {
        TODO()
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