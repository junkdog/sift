package sift.core.asm.ins

import br.usp.each.saeg.asm.defuse.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*
import sift.core.asm.type


fun dataflowAnalyze(cn: ClassNode, mn: MethodNode) {
    val analyzer = DefUseAnalyzer()
    analyzer.analyze(cn.type.internalName, mn)

    val variables: Array<Variable> = analyzer.variables
    val frames = analyzer.defUseFrames

    println("This method contains " + variables.size + " variables")
    for (i in 0 until mn.instructions.size()) {
        if (frames[i].definitions.isEmpty() && frames[i].uses.isEmpty())
            continue

        println("Instruction " + i + " contains definitions of " + frames[i].definitions)
        println("Instruction " + i + " contains usage of " + frames[i].uses)
    }
}


fun flowAnalyze(cn: ClassNode, mn: MethodNode) {
    val analyzer: FlowAnalyzer<BasicValue> = FlowAnalyzer(BasicInterpreter())
    analyzer.analyze(cn.type.internalName, mn)

    val successors = analyzer.successors
    val predecessors = analyzer.predecessors
    val basicBlocks = analyzer.basicBlocks
    val leaders = analyzer.leaders

    for (i in 0 until mn.instructions.size()) {
        // successors[i] array contains the indexes of the successors of instruction i
        println("Instruction " + i + " has " + successors[i].size + " successors")

        // predecessors[i] array contains the indexes of the predecessors of instruction i
        println("Instruction " + i + " has " + predecessors[i].size + " predecessors")
        println("Instruction " + i + " belongs to basic block " + leaders[i])
    }
    for (i in basicBlocks.indices) {
        println("Basic block " + i + " contains " + basicBlocks[i].size + " instructions")
    }
}

fun defintionUseCase(cn: ClassNode, mn: MethodNode) {
    val interpreter = DefUseInterpreter()
    val flowAnalyzer = FlowAnalyzer(interpreter)
    val analyzer = DefUseAnalyzer(flowAnalyzer, interpreter)
    analyzer.analyze(cn.type.internalName, mn)

    val variables: Array<Variable> = analyzer.variables
    val chains = DepthFirstDefUseChainSearch().search(
        analyzer.defUseFrames,
        analyzer.variables,
        flowAnalyzer.getSuccessors(),
        flowAnalyzer.getPredecessors()
    )

    println("This method contains " + chains.size + " Definition-Use Chains")
    fun debugVariable(variable: Variable): String {
        return "${variable.type.className} (${variable.insns::class.simpleName}) (${variable.variables.map { it::class.simpleName }})"
    }

    for (i in chains.indices) {
        val chain = chains[i]
        println("Instruction " + chain.def + " define variable " + debugVariable(variables[chain.`var`]))
        println("Instruction " + chain.use + " uses variable " + debugVariable(variables[chain.`var`]))
        // There is a path between chain.def and chain.use that not redefine chain.var
    }
}


//class DataFlowInterpreter : BasicInterpreter(ASM9) {
//override fun newOperation(insn: AbstractInsnNode): BasicValue {
//        return when (insn.opcode) {
//            Opcodes.ACONST_NULL -> BasicValue(null)
//            Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3,
//            Opcodes.ICONST_4, Opcodes.ICONST_5 -> BasicValue(Opcodes.INTEGER)
//            Opcodes.LCONST_0, Opcodes.LCONST_1 -> BasicValue(Opcodes.LONG)
//            Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 -> BasicValue(Opcodes.FLOAT)
//            Opcodes.DCONST_0, Opcodes.DCONST_1 -> BasicValue(Opcodes.DOUBLE)
//            Opcodes.BIPUSH, Opcodes.SIPUSH -> BasicValue(Opcodes.INTEGER)
//            Opcodes.LDC -> {
//                val cst = (insn as LdcInsnNode).cst
//                if (cst is Int) {
//                    BasicValue(Opcodes.INTEGER)
//                } else if (cst is Float) {
//                    BasicValue(Opcodes.FLOAT)
//                } else if (cst is Long) {
//                    BasicValue(Opcodes.LONG)
//                } else if (cst is Double) {
//                    BasicValue(Opcodes.DOUBLE)
//                } else if (cst is String) {
//                    BasicValue("java/lang/String")
//                } else if (cst is Type) {
//                    when (cst.sort) {
//                        Type.OBJECT -> BasicValue(cst.internalName)
//                        Type.ARRAY -> BasicValue(cst.descriptor)
//                        Type.METHOD -> BasicValue(cst.descriptor)
//                        else -> throw IllegalArgumentException("Illegal LDC constant " + cst)
//                    }
//                } else if (cst is Handle) {
//                    BasicValue("java/lang/invoke/MethodHandle")
//                } else {
//                    throw IllegalArgumentException("Illegal LDC constant " + cst)
//                }
//            }
//            Opcodes.JSR -> BasicValue(Opcodes.RETURN)
//            Opcodes.GETSTATIC -> BasicValue((insn as FieldInsnNode).owner)
//            Opcodes.NEW -> BasicValue("java/lang/Object")
//            else -> super.newOperation(insn)
//        }
//    }
//
//    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue {
//        return value
//    }
//
//    override fun
//}