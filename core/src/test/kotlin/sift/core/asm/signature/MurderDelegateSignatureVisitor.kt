package sift.core.asm.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor

class MurderDelegateSignatureVisitor(
    api: Int = Opcodes.ASM9,
) : SignatureVisitor(api) {

    override fun visitFormalTypeParameter(name: String) {
        error("no")
    }

    override fun visitClassBound(): SignatureVisitor {
        error("no")
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        error("no")
    }

    override fun visitSuperclass(): SignatureVisitor {
        error("no")
    }

    override fun visitInterface(): SignatureVisitor {
        error("no")
    }

    override fun visitParameterType(): SignatureVisitor {
        error("no")
    }

    override fun visitReturnType(): SignatureVisitor {
        error("no")
    }

    override fun visitExceptionType(): SignatureVisitor {
        error("no")
    }

    override fun visitBaseType(descriptor: Char) {
        error("no")
    }

    override fun visitTypeVariable(name: String) {
        error("no")
    }

    override fun visitArrayType(): SignatureVisitor {
        error("no")
    }

    override fun visitClassType(name: String) {
        error("no")
    }

    override fun visitInnerClassType(name: String) {
        error("no")
    }

    override fun visitTypeArgument() {
        error("no")
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        error("no")
    }

    override fun visitEnd() {
        error("no")
    }
}