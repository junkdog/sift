package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor
import sift.core.dsl.Type

internal class TypeArgumentVisitor(
    val onTypeArgument: (TypeSignature) -> Unit,
    val formalTypeParameters: (String) -> FormalTypeParameter?,
    val arrayDepth: Int = 0,
    api: Int = Opcodes.ASM9,
    signatureVisitor: SignatureVisitor? = null,
) : BaseSignatureVisitor(api, signatureVisitor) {

    private var arg: TypeSignature? = null

    // recursive; initial TypeArgumentVisitor created earlier
    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        requireNotNull(arg)

        return TypeArgumentVisitor(
            arg!!.args::add,
            formalTypeParameters,
            arrayDepth,
            api,
            sv?.visitTypeArgument(wildcard)
        )
    }

    override fun visitTypeArgument() {
        // ignored
    }

    override fun visitTypeVariable(name: String) {
        require(arg == null)

        val param = formalTypeParameters(name) ?: return
        arg = TypeSignature(ArgType.Var(param), arrayDepth, MetaType.GenericType)
            .also(onTypeArgument)

        sv?.visitTypeVariable(name)
    }

    override fun visitClassType(name: String) {
        require(arg == null)

        val type = Type.from(name)
        arg = TypeSignature(ArgType.Plain(type), arrayDepth, MetaType.Class)
            .also(onTypeArgument)

        sv?.visitClassType(name)
    }

    override fun visitArrayType(): SignatureVisitor {
        require(arg == null)

        return TypeArgumentVisitor(
            onTypeArgument = onTypeArgument,
            formalTypeParameters = formalTypeParameters,
            arrayDepth = arrayDepth + 1,
            api = api,
            signatureVisitor = sv?.visitArrayType()
        )
    }

    override fun visitBaseType(descriptor: Char) {
        val type = Type.primitiveType(descriptor)
        arg = TypeSignature(ArgType.Plain(type), arrayDepth, MetaType.Class)
            .also(onTypeArgument)

        sv?.visitBaseType(descriptor)
    }

    override fun visitInnerClassType(name: String) {
        error("test me")
        sv?.visitInnerClassType(name)
    }

    override fun visitEnd() {
        sv?.visitEnd()
    }
}
