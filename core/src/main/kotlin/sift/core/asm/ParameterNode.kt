package sift.core.asm

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.LocalVariableNode
import sift.core.api.AsmParameterNode


class ParameterNode(
    name: String,
    access: Int,
    val type: Type,
    val annotations: List<AnnotationNode>,
    val source: Source
) : AsmParameterNode(name, access) {
    constructor(
        ref: AsmParameterNode,
        type: Type,
        annotations: List<AnnotationNode>,
    ) : this(ref.name, ref.access, type, annotations, Source.Parameter)

    constructor(
        ref: LocalVariableNode,
        type: Type,
        annotations: List<AnnotationNode>
    ) : this(ref.name, 0, type, annotations, Source.LocalVariable)

    constructor(
        index: Int,
        type: Type,
        annotations: List<AnnotationNode>
    ) : this("${type.simpleName.camelCase}$index", 0, type, annotations, Source.NoDebugInfo)

    enum class Source { LocalVariable, Parameter, NoDebugInfo }
}
private val String.camelCase: String
    get() = "${this[0].lowercase()}${substring(1)}"
