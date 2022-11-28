package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode


fun ClassNode.signature(wrap: SignatureVisitor? = null): ClassSignatureNode? {
    signature ?: return null

    return SignatureParser(Opcodes.ASM9, wrap)
        .also { SignatureReader(signature).accept(it) }
        .asClassSignatureNode
}

fun FieldNode.signature(
    formalTypeParams: List<FormalTypeParameter>,
    wrap: SignatureVisitor? = null
): TypeSignatureNode? {
    signature ?: return null

    return SignatureParser(formalTypeParams, Opcodes.ASM9, wrap)
        .also { SignatureReader(signature).accept(it) }
        .asTypeSignatureNode
}

fun MethodNode.signature(
    formalTypeParams: List<FormalTypeParameter>,
    wrap: SignatureVisitor? = null,
): MethodSignatureNode? {
    signature ?: return null

    return SignatureParser(formalTypeParams, Opcodes.ASM9, wrap)
        .also { SignatureReader(signature).accept(it) }
        .asMethodSignatureNode
}

data class MethodSignatureNode(
    val formalParameters: List<FormalTypeParameter>,
    val methodParameters: List<TypeSignature>,
    val returnType: TypeSignature?,
)

data class TypeSignatureNode(
    val formalParameters: List<FormalTypeParameter>,
    val extends: List<TypeSignature>
)

data class ClassSignatureNode(
    val formalParameters: List<FormalTypeParameter>,
    val extends: TypeSignature,
    val implements: List<TypeSignature>
) {
    constructor(
        parameters: List<FormalTypeParameter>,
        extends: List<TypeSignature>
    ) : this(parameters, extends.first(), extends.drop(1))
}
