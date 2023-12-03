package sift.core.asm.ins

import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import sift.core.terminal.Gruvbox.aqua1
import sift.core.terminal.Gruvbox.aqua2
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.fg
import sift.core.terminal.Gruvbox.gray
import sift.core.terminal.Gruvbox.green1
import sift.core.terminal.Gruvbox.orange2
import sift.core.terminal.Gruvbox.purple1
import sift.core.terminal.Gruvbox.purple2
import sift.core.terminal.Gruvbox.red1
import sift.core.terminal.Gruvbox.yellow1
import sift.core.terminal.Gruvbox.yellow2
import sift.core.terminal.TextTransformer
import kotlin.reflect.KProperty

val labelShortener = TextTransformer.idSequence(Regex("L[0-9]+"))


internal fun renderInstruction(ins: AbstractInsnNode): String {
    return when (ins) {
        is MethodInsnNode -> when (ins.opcode) {
            Opcodes.INVOKEVIRTUAL   -> "INVOKEVIRTUAL"
            Opcodes.INVOKESTATIC    -> "INVOKESTATIC"
            Opcodes.INVOKEINTERFACE -> "INVOKEINTERFACE"
            Opcodes.INVOKESPECIAL   -> "INVOKESPECIAL"
            else -> missingOpcodeError(ins)
        }.let { "$it ${ins.owner}.${ins.name}${ins.desc}" }
        is FieldInsnNode -> when (ins.opcode) {
            Opcodes.GETFIELD  -> "GETFIELD"
            Opcodes.GETSTATIC -> "GETSTATIC"
            Opcodes.PUTFIELD  -> "PUTFIELD"
            Opcodes.PUTSTATIC -> "PUTSTATIC"
            else -> missingOpcodeError(ins)
        }.let { "$it ${ins.owner}.${ins.name}: ${ins.desc}" }
        is TableSwitchInsnNode -> when (ins.opcode) {
            Opcodes.TABLESWITCH -> "TABLESWITCH"
            else -> missingOpcodeError(ins)
        }
        is LineNumberNode -> "LINE ${ins.line}"
        is IincInsnNode -> when (ins.opcode) {
            Opcodes.IINC -> "IINC"
            else -> missingOpcodeError(ins)
        }
        is IntInsnNode -> when (ins.opcode) {
            Opcodes.BIPUSH -> "BIPUSH"
            Opcodes.SIPUSH -> "SIPUSH"
            Opcodes.NEWARRAY -> "NEWARRAY"
            else -> missingOpcodeError(ins)
        }.let { "$it ${ins.operand}" }
        is LabelNode -> "LABEL @${labelShortener(ins.label.toString())}"
        is LdcInsnNode -> when (ins.opcode) {
            Opcodes.LDC -> "LDC ${ins.cst}"
            else -> missingOpcodeError(ins)
        }
        is TypeInsnNode -> when (ins.opcode) {
            Opcodes.NEW -> "NEW"
            Opcodes.ANEWARRAY -> "ANEWARRAY"
            Opcodes.CHECKCAST -> "CHECKCAST"
            Opcodes.INSTANCEOF -> "INSTANCEOF"
            else -> missingOpcodeError(ins)
        }.let { "$it ${ins.desc}" }
        is VarInsnNode -> when (ins.opcode) {
            Opcodes.ILOAD -> "ILOAD"
            Opcodes.LLOAD -> "LLOAD"
            Opcodes.FLOAD -> "FLOAD"
            Opcodes.DLOAD -> "DLOAD"
            Opcodes.ALOAD -> "ALOAD"
            Opcodes.ISTORE -> "ISTORE"
            Opcodes.LSTORE -> "LSTORE"
            Opcodes.FSTORE -> "FSTORE"
            Opcodes.DSTORE -> "DSTORE"
            Opcodes.ASTORE -> "ASTORE"
            Opcodes.RET -> "RET"
            else -> missingOpcodeError(ins)
        }.let { "$it ${ins.`var`}" }
        is InvokeDynamicInsnNode -> when (ins.opcode) {
            Opcodes.INVOKEDYNAMIC -> "INVOKEDYNAMIC ${ins.name} ${ins.desc}"
            else -> missingOpcodeError(ins)
        }
        is FrameNode -> when (ins.type) {
            Opcodes.F_NEW -> "F_NEW"
            Opcodes.F_FULL -> "F_FULL"
            Opcodes.F_APPEND -> "F_APPEND"
            Opcodes.F_CHOP -> "F_CHOP"
            Opcodes.F_SAME -> "F_SAME"
            Opcodes.F_SAME1 -> "F_SAME1"
            else -> missingOpcodeError(ins)
        }.let { "$it local=${ins.local?.map { it.toString().substringAfterLast("/") } ?: listOf()} stack=${ins.stack ?: listOf()}" }
        is JumpInsnNode -> when (ins.opcode) {
            Opcodes.IFEQ -> "IFEQ"
            Opcodes.IFNE -> "IFNE"
            Opcodes.IFLT -> "IFLT"
            Opcodes.IFGE -> "IFGE"
            Opcodes.IFGT -> "IFGT"
            Opcodes.IFLE -> "IFLE"
            Opcodes.IF_ICMPEQ -> "IF_ICMPEQ"
            Opcodes.IF_ICMPNE -> "IF_ICMPNE"
            Opcodes.IF_ICMPLT -> "IF_ICMPLT"
            Opcodes.IF_ICMPGE -> "IF_ICMPGE"
            Opcodes.IF_ICMPGT -> "IF_ICMPGT"
            Opcodes.IF_ICMPLE -> "IF_ICMPLE"
            Opcodes.IF_ACMPEQ -> "IF_ACMPEQ"
            Opcodes.IF_ACMPNE -> "IF_ACMPNE"
            Opcodes.GOTO -> "GOTO"
            Opcodes.JSR -> "JSR"
            Opcodes.IFNULL -> "IFNULL"
            Opcodes.IFNONNULL -> "IFNONNULL"
            else -> missingOpcodeError(ins)
        }.let { "$it @${labelShortener(ins.label.label.toString())}" }
        is LookupSwitchInsnNode -> when (ins.opcode) {
            Opcodes.LOOKUPSWITCH -> "LOOKUPSWITCH"
            else -> missingOpcodeError(ins)
        }
        is InsnNode -> when (ins.opcode) {
            Opcodes.NOP -> "NOP"
            // constants
            Opcodes.ACONST_NULL -> "ACONST_NULL"
            Opcodes.ICONST_M1 -> "ICONST_M1"
            Opcodes.ICONST_0 -> "ICONST_0"
            Opcodes.ICONST_1 -> "ICONST_1"
            Opcodes.ICONST_2 -> "ICONST_2"
            Opcodes.ICONST_3 -> "ICONST_3"
            Opcodes.ICONST_4 -> "ICONST_4"
            Opcodes.ICONST_5 -> "ICONST_5"
            Opcodes.LCONST_0 -> "LCONST_0"
            Opcodes.LCONST_1 -> "LCONST_1"
            Opcodes.FCONST_0 -> "FCONST_0"
            Opcodes.FCONST_1 -> "FCONST_1"
            Opcodes.FCONST_2 -> "FCONST_2"
            Opcodes.DCONST_0 -> "DCONST_0"
            Opcodes.DCONST_1 -> "DCONST_1"
            // load and store
            Opcodes.IALOAD -> "IALOAD"
            Opcodes.LALOAD -> "LALOAD"
            Opcodes.FALOAD -> "FALOAD"
            Opcodes.DALOAD -> "DALOAD"
            Opcodes.AALOAD -> "AALOAD"
            Opcodes.BALOAD -> "BALOAD"
            Opcodes.CALOAD -> "CALOAD"
            Opcodes.SALOAD -> "SALOAD"
            Opcodes.IASTORE -> "IASTORE"
            Opcodes.LASTORE -> "LASTORE"
            Opcodes.FASTORE -> "FASTORE"
            Opcodes.DASTORE -> "DASTORE"
            Opcodes.AASTORE -> "AASTORE"
            Opcodes.BASTORE -> "BASTORE"
            Opcodes.CASTORE -> "CASTORE"
            Opcodes.SASTORE -> "SASTORE"
            // stack
            Opcodes.POP -> "POP"
            Opcodes.POP2 -> "POP2"
            Opcodes.DUP -> "DUP"
            Opcodes.DUP_X1 -> "DUP_X1"
            Opcodes.DUP_X2 -> "DUP_X2"
            Opcodes.DUP2 -> "DUP2"
            Opcodes.DUP2_X1 -> "DUP2_X1"
            Opcodes.DUP2_X2 -> "DUP2_X2"
            Opcodes.SWAP -> "SWAP"
            // arithmetic
            Opcodes.IADD -> "IADD"
            Opcodes.LADD -> "LADD"
            Opcodes.FADD -> "FADD"
            Opcodes.DADD -> "DADD"
            Opcodes.ISUB -> "ISUB"
            Opcodes.LSUB -> "LSUB"
            Opcodes.FSUB -> "FSUB"
            Opcodes.DSUB -> "DSUB"
            Opcodes.IMUL -> "IMUL"
            Opcodes.LMUL -> "LMUL"
            Opcodes.FMUL -> "FMUL"
            Opcodes.DMUL -> "DMUL"
            Opcodes.IDIV -> "IDIV"
            Opcodes.LDIV -> "LDIV"
            Opcodes.FDIV -> "FDIV"
            Opcodes.DDIV -> "DDIV"
            Opcodes.IREM -> "IREM"
            Opcodes.LREM -> "LREM"
            Opcodes.FREM -> "FREM"
            Opcodes.DREM -> "DREM"
            Opcodes.INEG -> "INEG"
            Opcodes.LNEG -> "LNEG"
            Opcodes.FNEG -> "FNEG"
            Opcodes.DNEG -> "DNEG"
            // shift
            Opcodes.ISHL -> "ISHL"
            Opcodes.LSHL -> "LSHL"
            Opcodes.ISHR -> "ISHR"
            Opcodes.LSHR -> "LSHR"
            Opcodes.IUSHR -> "IUSHR"
            Opcodes.LUSHR -> "LUSHR"
            // bitwise
            Opcodes.IAND -> "IAND"
            Opcodes.LAND -> "LAND"
            Opcodes.IOR -> "IOR"
            Opcodes.LOR -> "LOR"
            Opcodes.IXOR -> "IXOR"
            Opcodes.LXOR -> "LXOR"
            // convert int to other types
            Opcodes.I2L -> "I2L"
            Opcodes.I2F -> "I2F"
            Opcodes.I2D -> "I2D"
            // convert long to other types
            Opcodes.L2I -> "L2I"
            Opcodes.L2F -> "L2F"
            Opcodes.L2D -> "L2D"
            // convert float to other types
            Opcodes.F2I -> "F2I"
            Opcodes.F2L -> "F2L"
            Opcodes.F2D -> "F2D"
            Opcodes.D2I -> "D2I"
            Opcodes.D2L -> "D2L"
            Opcodes.D2F -> "D2F"
            // convert int to other types
            Opcodes.I2B -> "I2B"
            Opcodes.I2C -> "I2C"
            Opcodes.I2S -> "I2S"
            // compare
            Opcodes.LCMP -> "LCMP"
            Opcodes.FCMPL -> "FCMPL"
            Opcodes.FCMPG -> "FCMPG"
            Opcodes.DCMPL -> "DCMPL"
            Opcodes.DCMPG -> "DCMPG"
            // return
            Opcodes.IRETURN -> "IRETURN"
            Opcodes.LRETURN -> "LRETURN"
            Opcodes.FRETURN -> "FRETURN"
            Opcodes.DRETURN -> "DRETURN"
            Opcodes.ARETURN -> "ARETURN"
            Opcodes.RETURN -> "RETURN"
            Opcodes.ATHROW -> "ATHROW"
            // array
            Opcodes.ARRAYLENGTH -> "ARRAYLENGTH"
            // monitor
            Opcodes.MONITORENTER -> "MONITORENTER"
            Opcodes.MONITOREXIT -> "MONITOREXIT"

            else -> missingOpcodeError(ins)
        }
        else -> error("missing case for instruction ${ins::class.simpleName}")
    }
}


internal fun renderTerminal(ins: AbstractInsnNode, opcode: String): String {
    val trimmed = opcode.trim()

    val type = trimmed.substringBefore(" ")
        .let { if (ins !is LabelNode) it.padEnd(16, ' ') else it }

    val indentation = "    ".takeIf { ins !is LabelNode } ?: ""
    val args = trimmed.substringAfter(" ", "")

    return indentation + ins.labelStyle(type) + " ${ins.argStyle(args)}"
}

internal val AbstractInsnNode.argStyle: TextStyle
    get() = when (this) {
        is MethodInsnNode        -> yellow2
        is LabelNode             -> yellow2 + bold
        is JumpInsnNode          -> yellow2 + bold
        is LineNumberNode        -> gray
        else                     -> fg
    }

internal val AbstractInsnNode.labelStyle: TextStyle
    get() = when (this) {
        is MethodInsnNode        -> green1 + inverse
        is FieldInsnNode         -> blue2
        is TableSwitchInsnNode   -> purple1
        is LineNumberNode        -> gray
        is IincInsnNode          -> yellow1
        is IntInsnNode           -> orange2
        is LabelNode             -> yellow2 + bold
        is LdcInsnNode           -> yellow1
        is TypeInsnNode          -> aqua1 + bold
        is VarInsnNode           -> aqua2
        is InvokeDynamicInsnNode -> red1
        is FrameNode             -> purple2 + bold
        is JumpInsnNode          -> yellow2 + inverse
        is LookupSwitchInsnNode  -> yellow1
        is InsnNode              -> fg
        else -> error("missing case for instruction ${this::class.simpleName}")
    }

private fun missingOpcodeError(
    ins: AbstractInsnNode,
): Nothing {
    error("missing case for ${ins.opcode}, candidates: ${matchOpcode(ins)}")
}

private val screamingSnakecase = Regex("[A-Z0-9_]+")

@Suppress("UNCHECKED_CAST")
private fun matchOpcode(ins: AbstractInsnNode): List<String> {
    val opcodes = Opcodes::class.members
        .filter { prop -> screamingSnakecase.matches(prop.name) }
        .filter { prop -> prop.returnType.classifier == Int::class }
        .mapNotNull { prop -> prop as? KProperty<Int> }
        .map { prop -> prop.getter.call() to prop.name }
        .groupBy({ (opcode, _) -> opcode }, { (_, name) -> name })

    return opcodes[ins.opcode]
        ?: ins::class.simpleName.takeIf { ins.opcode == -1 }?.let(::listOf)
        ?: error("wtf ${ins.opcode}")
}
