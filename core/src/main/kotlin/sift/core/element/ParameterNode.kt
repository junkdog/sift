package sift.core.element

import org.objectweb.asm.tree.LocalVariableNode
import sift.core.AsmNodeHashcoder.hash
import sift.core.asm.signature.TypeSignature
import sift.core.asm.signature.signature
import sift.core.asm.simpleName
import sift.core.dsl.Type
import sift.core.kotlin.KotlinFunction
import sift.core.kotlin.KotlinParameter

class ParameterNode private constructor(
    private val cn: ClassNode,
    private val mn: MethodNode,
    private val kpn: KotlinParameter? = null,
    val name: String,
    val type: Type,
    val signature: TypeSignature?,
    override val annotations: List<AnnotationNode>,
    val source: Source
) : Element {
    val owner: MethodNode
        get() = mn

    private val hash = hash(cn, mn, name, type)

    override val simpleName: String
        get() = type.simpleName

    /** returns true if this parameter is a kotlin extension function receiver */
    val isReceiver: Boolean
        get() = kpn?.isExtensionReceiver == true

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = hash
    override fun toString(): String = "$mn($name: ${type.simpleName})"

    companion object {
        internal fun from(
            cn: ClassNode,
            mn: MethodNode,
            asmMn: AsmMethodNode,
            kfn: KotlinFunction?,
        ): List<ParameterNode> {
            val argumentTypes = AsmType.getArgumentTypes(asmMn.desc)
            val extensionFunctionOffset = kfn?.isExtension?.takeIf { it }?.let { 1 } ?: 0

            fun kotlinParameter(idx: Int): KotlinParameter? {
                return when {
                    extensionFunctionOffset == 1 && idx == 0 -> KotlinParameter.from(mn.receiver!!)
                    else -> kfn?.parameters?.getOrNull(idx - extensionFunctionOffset)
                }
            }

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
                        kotlinParameter(idx),
                        asmMn.parameters[idx].name,
                        Type.from(type),
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
                        kotlinParameter(idx),
                        localVar.name,
                        Type.from(argumentTypes[idx]),
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
                        kotlinParameter(idx),
                        "${type.simpleName.camelCase}$idx",
                        Type.from(type),
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