package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureVisitor

class TypeArgumentVisitor(
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
        // fixme: look into whether we need to act on <*> or somethong
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

        val type = Type.getType("L$name;")
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
        val type = when (descriptor) {
            'Z' -> Type.BOOLEAN_TYPE
            'B' -> Type.BYTE_TYPE
            'C' -> Type.CHAR_TYPE
            'S' -> Type.SHORT_TYPE
            'I' -> Type.INT_TYPE
            'J' -> Type.LONG_TYPE
            'F' -> Type.FLOAT_TYPE
            'D' -> Type.DOUBLE_TYPE
            'V' -> Type.VOID_TYPE
            else -> error(descriptor)
        }

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
