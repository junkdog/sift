package sift.core.asm.signature

import net.onedaybeard.collectionsby.firstBy
import org.objectweb.asm.signature.SignatureVisitor

class SignatureParser(
    api: Int,
    sv: SignatureVisitor? = null
) : BaseSignatureVisitor(api, sv) {

    private var type: ArgType? = null
    private var parameters: MutableList<FormalTypeParameter> = mutableListOf()
    private var args: MutableList<TypeArgument> = mutableListOf()
    private var returnType: TypeSignature? = null
    private var extends: MutableList<TypeSignature> = mutableListOf()

//    var node: SignatureNode? = SignatureNode()
//        private set

    override fun visitFormalTypeParameter(name: String) {
        parameters += FormalTypeParameter(name, null)
        sv?.visitFormalTypeParameter(name)
    }

    override fun visitClassBound(): SignatureVisitor {
        return FormalTypeParameterVisitor(
            parameters.last(),
            MetaType.Class,
            parameters::firstByName,
            parameters::add,
            api,
            sv?.visitClassBound()
        )
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        return FormalTypeParameterVisitor(
            parameters.last(),
            MetaType.Interface,
            parameters::firstByName,
            parameters::add,
            api,
            sv?.visitInterfaceBound()
        )
    }

    override fun visitSuperclass(): SignatureVisitor {
        return TODO()
    }

}

internal fun MutableList<FormalTypeParameter>.firstByName(
    name: String
) = firstBy(FormalTypeParameter::name, name)