package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor
import sift.core.dsl.Type

internal class FormalTypeParameterVisitor(
    val parameter: FormalTypeParameter,
    val lookup: (String) -> FormalTypeParameter,
    api: Int = Opcodes.ASM9,
    signatureVisitor: SignatureVisitor? = null,
) : BaseSignatureVisitor(api, signatureVisitor) {

    override fun visitClassType(name: String) {
        parameter.extends += TypeSignature(ArgType.Plain(Type.from(name)), 0, MetaType.Class)
        sv?.visitClassType(name)
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        return TypeArgumentVisitor(
            parameter.extends.last().args::add,
            lookup,
            0,
            api,
            sv?.visitTypeArgument(wildcard)
        )
    }

    override fun visitTypeArgument() {
        sv?.visitTypeArgument()
    }
}