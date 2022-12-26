package sift.core.asm.signature

import org.objectweb.asm.signature.SignatureVisitor
import java.util.*

open class BaseSignatureVisitor(
    api: Int,
    val sv: SignatureVisitor?
) : SignatureVisitor(api) {

    var indent: Int = 0
    private var tag: String? = UUID.randomUUID().toString().takeLast(6)

    override fun visitFormalTypeParameter(name: String) {
        error("visitFormalTypeParameter")
    }

    override fun visitClassBound(): SignatureVisitor {
        error("visitClassBound()")
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        error("visitInterfaceBound()")
    }

    override fun visitSuperclass(): SignatureVisitor {
        error("visitSuperclass()")
    }

    override fun visitInterface(): SignatureVisitor {
        error("visitInterface()")
    }

    override fun visitParameterType(): SignatureVisitor {
        error("visitParameterType()")
    }

    override fun visitReturnType(): SignatureVisitor {
        error("visitReturnType()")
    }

    override fun visitExceptionType(): SignatureVisitor {
        error("visitExceptionType()")
    }

    override fun visitBaseType(descriptor: Char) {
        error("visitBaseType(descriptor=$descriptor)")
    }

    override fun visitTypeVariable(name: String) {
        error("visitTypeVariable(name=$name)")
    }

    override fun visitArrayType(): SignatureVisitor {
        error("visitArrayType()")
    }

    override fun visitClassType(name: String) {
        error("visitClassType(name=$name)")
    }

    override fun visitInnerClassType(name: String) {
        error("visitInnerClassType(name=$name)")
    }

    override fun visitTypeArgument() {
        error("visitTypeArgument()")
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        error("visitTypeArgument(wildcard=$wildcard)")
    }

    override fun visitEnd() {
        sv?.visitEnd()
    }

    fun log(s: String) {
        val t = "$tag "

        val prefix = "".padEnd(maxOf(0, indent) * 4)
        println(t + prefix + s)
    }
}