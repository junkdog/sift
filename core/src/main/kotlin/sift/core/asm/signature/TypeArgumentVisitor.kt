package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureVisitor

class TypeArgumentVisitor(
    val onTypeArgument: (TypeSignature) -> Unit,
    val formalTypeParameters: (String) -> FormalTypeParameter,
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
            api,
            sv?.visitTypeArgument(wildcard)
        )
    }

    override fun visitTypeVariable(name: String) {
        require(arg == null)

        val param = formalTypeParameters(name)
        arg = TypeSignature(ArgType.Var(param), MetaType.GenericType)
            .also(onTypeArgument)

        sv?.visitTypeVariable(name)
    }

    override fun visitClassType(name: String) {
        require(arg == null)

        val type = Type.getType("L$name;")
        arg = TypeSignature(ArgType.Plain(type), MetaType.Class)
            .also(onTypeArgument)

        sv?.visitClassType(name)
    }

    override fun visitArrayType(): SignatureVisitor {
        require(arg == null)
//
//        val type = Type.getType("L$name;")
//        val argType = ArgType.Array(null)
//        arg = TypeArgument(argType, MetaType.Array, 'X')
//            .also(onTypeArgument)

        return TypeArgumentVisitor(
            onTypeArgument = onTypeArgument, // FIXME: don't ignore arrays
            formalTypeParameters = formalTypeParameters,
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

        arg = TypeSignature(ArgType.Plain(type), MetaType.Class)
            .also(onTypeArgument)

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
