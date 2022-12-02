package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureVisitor

class FormalTypeParameterVisitor(
    val parameter: FormalTypeParameter,
    val lookup: (String) -> FormalTypeParameter,
    api: Int = Opcodes.ASM9,
    signatureVisitor: SignatureVisitor? = null,
) : BaseSignatureVisitor(api, signatureVisitor) {

    override fun visitClassType(name: String) {
        parameter.extends += Type.getType("L$name;")
        sv?.visitClassType(name)
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        return TypeArgumentVisitor(
            parameter.args::add,
            lookup,
            0,
            api,
            sv?.visitTypeArgument(wildcard)
        )
    }
}