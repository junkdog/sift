package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor

class LoggingTypeSignatureVisitor(
    api: Int = Opcodes.ASM9,
    signatureVisitor: SignatureVisitor? = null
) : BaseSignatureVisitor(api, signatureVisitor) {
    override fun visitClassBound(): SignatureVisitor {
        log("visitClassBound()")
        sv?.visitClassBound()
        return LoggingTypeSignatureVisitor()
            .also { it.indent = indent + 1 }
    }

    override fun visitBaseType(descriptor: Char) {
        log("visitBaseType(descriptor=$descriptor)")
        sv?.visitBaseType(descriptor)
    }

    override fun visitTypeVariable(name: String) {
        log("visitTypeVariable(name=$name)")
        sv?.visitTypeVariable(name)
    }

    override fun visitArrayType(): SignatureVisitor {
        log("visitArrayType()")
        sv?.visitArrayType()
        return LoggingTypeSignatureVisitor()
            .also { it.indent = indent + 1 }
    }

    override fun visitClassType(name: String) {
        log("visitClassType(name=$name)")
        sv?.visitClassType(name)
    }

    override fun visitInnerClassType(name: String) {
        log("visitInnerClassType(name=$name)")
        sv?.visitInnerClassType(name)
    }

    override fun visitTypeArgument() {
        log("visitTypeArgument()")
        sv?.visitTypeArgument()
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        log("visitTypeArgument(wildcard=$wildcard)")
        sv?.visitTypeArgument(wildcard)
        return LoggingTypeSignatureVisitor()
            .also { it.indent = indent + 1 }
    }
}