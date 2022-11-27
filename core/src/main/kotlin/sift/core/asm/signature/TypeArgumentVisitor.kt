package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureVisitor

class TypeArgumentVisitor(
    val onTypeArgument: (TypeArgument) -> Unit,
    val formalTypeParameters: (String) -> FormalTypeParameter,
    api: Int = Opcodes.ASM9,
    signatureVisitor: SignatureVisitor? = null,
) : BaseSignatureVisitor(api, signatureVisitor) {

    private var arg: TypeArgument? = null

    // recursive; initial TypeArgumentVisitor created earlier
    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        requireNotNull(arg)

        return TypeArgumentVisitor(arg!!.inner::add, formalTypeParameters, api, sv?.visitTypeArgument(wildcard))
    }

    override fun visitTypeVariable(name: String) {
        require(arg == null)

        val param = formalTypeParameters(name)
        arg = TypeArgument(ArgType.Var(param), MetaType.GenericType, 'X')
            .also(onTypeArgument)

        sv?.visitTypeVariable(name)
    }

    override fun visitClassType(name: String) {
        require(arg == null)

        val type = Type.getType("L$name;")
        arg = TypeArgument(ArgType.Plain(type), MetaType.Class, 'X')
            .also(onTypeArgument)

        sv?.visitClassType(name)
    }

    override fun visitArrayType(): SignatureVisitor {
        TODO("test me")
        return sv?.visitArrayType()!!
    }

    override fun visitBaseType(descriptor: Char) {
        TODO("test me")
        sv?.visitBaseType(descriptor)
    }

    override fun visitInnerClassType(name: String) {
        TODO("test me")
        sv?.visitInnerClassType(name)
    }

    override fun visitEnd() {
        sv?.visitEnd()
    }
}