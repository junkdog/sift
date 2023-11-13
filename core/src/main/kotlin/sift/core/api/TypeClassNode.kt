package sift.core.api

import sift.core.asm.signature.ArgType
import sift.core.asm.signature.FormalTypeParameter
import sift.core.asm.signature.TypeParameter
import sift.core.asm.signature.TypeSignature
import sift.core.dsl.Type
import sift.core.element.ClassNode
import sift.core.element.FieldNode
import sift.core.element.MethodNode
import sift.core.element.toType
import sift.core.tree.Tree

internal data class TypeClassNode(
    val type: Type,
    val cn: ClassNode?,
    val isInterface: Boolean,
    var generics: List<TypeParameter>? = null
) {
    constructor(type: Type, cn: ClassNode)
        : this(type, cn, cn.isInterface)
    override fun toString(): String = type.simpleName
}

internal fun Tree<TypeClassNode>.fields(): List<FieldNode> {
    val cn = value.cn ?: return listOf()
    val ftps = value.generics!!.associateBy(TypeParameter::name)
    return cn.fields.map { fn -> fn.reify(ftps) }
}

/** propagates generic types from parent to children */
internal fun Tree<TypeClassNode>.resolveGenerics() {
    // only need to resolve generics once
    if (value.generics != null)
        return

    fun update(node: Tree<TypeClassNode>) {
        val propagatedGenerics = node.parent?.value?.generics ?: listOf()
        var genericTypes = node.genericTypes()
        if (propagatedGenerics.isNotEmpty() && genericTypes.isNotEmpty()) {
            val lookup = propagatedGenerics.associateBy { Type.from(it.name) }
            genericTypes = genericTypes.mapValues { (_, v) -> lookup[v.bound]?.let { v.copy(bound = it.bound) } ?: v }
        }

        node.value.generics = genericTypes.map { (_, v) -> v }
    }

    walk().forEach(::update)
}



// with resolved generic types methods
internal fun Tree<TypeClassNode>.methods(): List<MethodNode> {
    val cn = value.cn ?: return listOf()
    val ftps = value.generics!!.associateBy(TypeParameter::name)
    return cn.methods.map { mn -> mn.reify(ftps)  }
}

private fun Tree<TypeClassNode>.genericTypes(): Map<String, TypeParameter> {
    val signature = value.cn?.signature ?: return mapOf()

    val innerTypes = value.type.innerTypes.takeIf(List<Type>::isNotEmpty)
        ?: signature.formalParameters
        .map(FormalTypeParameter::name)
        .map(Type::from)
        .takeIf(List<Type>::isNotEmpty) // fixme: when only some type parameters are specified
        ?: return mapOf()


    val ftps = signature.formalParameters
        .mapIndexed { i, ftp -> TypeParameter(ftp.name, innerTypes[i], ftp.extends.map { it.toType() }) }
        .associateBy(TypeParameter::name)

    // todo: fix or can we ignore interfaces?
    val interfaces = signature.implements
        .flatMap(TypeSignature::args)
        .mapNotNull { it.argType as? ArgType.Var }

    return ftps
}
