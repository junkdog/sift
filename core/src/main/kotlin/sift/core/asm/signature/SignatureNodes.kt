package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import sift.core.element.AsmMethodNode


// todo; param signature node

internal fun ClassNode.signature(wrap: SignatureVisitor? = null): ClassSignatureNode? {
    signature ?: return null

    return SignatureParser(Opcodes.ASM9, wrap)
        .also { SignatureReader(signature).accept(it) }
        .asClassSignatureNode
}

internal fun FieldNode.signature(
    formalTypeParams: List<FormalTypeParameter>,
    wrap: SignatureVisitor? = null
): FieldSignatureNode? {
    signature ?: return null

    return SignatureParser(formalTypeParams, Opcodes.ASM9, wrap)
        .also { SignatureReader(signature).accept(it) }
        .asFieldSignatureNode
}

internal fun AsmMethodNode.signature(
    formalTypeParams: List<FormalTypeParameter>,
    wrap: SignatureVisitor? = null,
): MethodSignatureNode? {
    signature ?: return null

    return try {
        SignatureParser(formalTypeParams, Opcodes.ASM9, wrap)
            .also { SignatureReader(signature).accept(it) }
            .asMethodSignatureNode
    } catch (e: SignatureParsingException) {
        // FIXME: callsite-aware signatures not handled at the moment
        // currently ignored
        null
    }
}

internal data class ClassSignatureNode(
    val formalParameters: List<FormalTypeParameter>,
    val extends: TypeSignature,
    val implements: List<TypeSignature>
) {
    constructor(
        parameters: List<FormalTypeParameter>,
        extends: List<TypeSignature>
    ) : this(parameters, extends.first(), extends.drop(1))
}

data class FieldSignatureNode(
    val formalParameters: List<FormalTypeParameter>,
    val extends: TypeSignature
)

data class MethodSignatureNode(
    val formalParameters: List<FormalTypeParameter>,
    val methodParameters: List<TypeSignature>,
    val returns: TypeSignature,
)

data class TypeSignatureNode(
    val formalParameters: List<FormalTypeParameter>,
    val extends: List<TypeSignature>
)

