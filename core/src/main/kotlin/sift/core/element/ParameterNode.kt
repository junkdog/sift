package sift.core.element

import org.objectweb.asm.Type
import org.objectweb.asm.tree.LocalVariableNode
import sift.core.AsmNodeHashcoder.hash
import sift.core.asm.signature.TypeSignature
import sift.core.asm.signature.signature
import sift.core.asm.simpleName

class ParameterNode private constructor(
    private val cn: ClassNode,
    private val mn: MethodNode,
    val name: String,
    val type: AsmType,
    val signature: TypeSignature?,
    override val annotations: List<AnnotationNode>,
    val source: Source
) : Element {
    val owner: MethodNode
        get() = mn

    override val simpleName: String
        get() = type.simpleName

    override fun equals(other: Any?): Boolean {
        return other is ParameterNode
            && cn == other.cn
            && mn == other.mn
            && name == other.name
            && type == other.type
    }

    override fun hashCode(): Int {
        return hash(cn, mn, name, type)
    }

    override fun toString(): String = "$mn($name: ${type.simpleName})"

    companion object {
        fun from(cn: ClassNode, mn: MethodNode, asmMn: AsmMethodNode): List<ParameterNode> {
            val argumentTypes = Type.getArgumentTypes(asmMn.desc)

            val annotations: List<List<AsmAnnotationNode>> by lazy {
                val visible = asmMn.visibleParameterAnnotations?.toList()
                val invisible = asmMn.invisibleParameterAnnotations?.toList()

                when {
                    visible == null -> invisible?.map { it ?: listOf() }
                        ?: List(argumentTypes.size) { listOf() }

                    invisible == null -> visible.map { it ?: listOf() }
                    else -> visible.zip(invisible) { a, b -> (a ?: listOf()) + (b ?: listOf()) }
                }
            }

            val signatures = asmMn.signature(mn.formalTypeParameters)?.methodParameters

            return when {
                // no parameters to resolve
                argumentTypes.isEmpty() -> listOf()

                // convert ASM parameter nodes to sift's
                asmMn.parameters?.isNotEmpty() == true -> argumentTypes
                    .zip(annotations)
                    .mapIndexed { idx, (type, anno) -> ParameterNode(
                        cn,
                        mn,
                        asmMn.parameters[idx].name,
                        type,
                        signatures?.getOrNull(idx),
                        anno.map(AnnotationNode::from),
                        Source.Parameter
                    ) }

                // create parameters from localvars
                asmMn.localVariables?.isNotEmpty() == true -> asmMn.localVariables
                    .sortedBy(LocalVariableNode::index)
                    .drop(1) // FIXME: 'this' assumed at index 0
                    .take(argumentTypes.size)
                    .zip(annotations)
                    .mapIndexed { idx, (localVar, anno) -> ParameterNode(
                        cn,
                        mn,
                        localVar.name,
                        argumentTypes[idx],
                        signatures?.getOrNull(idx),
                        anno.map(AnnotationNode::from),
                        Source.LocalVariable
                    ) }

                // fallback: create parameters with names from type
                else -> argumentTypes
                    .zip(annotations)
                    .mapIndexed { idx, (type, ans) -> ParameterNode(
                        cn,
                        mn,
                        "${type.simpleName.camelCase}$idx",
                        type,
                        signatures?.getOrNull(idx),
                        ans.map(AnnotationNode::from),
                        Source.NoDebugInfo
                    ) }
            }
        }
    }

    enum class Source { LocalVariable, Parameter, NoDebugInfo }
}

private val String.camelCase: String
    get() = "${this[0].lowercase()}${substring(1)}"