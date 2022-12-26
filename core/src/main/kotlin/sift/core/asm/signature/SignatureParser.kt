package sift.core.asm.signature

import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.signature.SignatureVisitor

/**
 * Parses a generic signature string into a [TypeSignature] object.
 */
class SignatureParser(
    typeParams: List<FormalTypeParameter>,
    api: Int,
    sv: SignatureVisitor? = null
) : BaseSignatureVisitor(api, sv) {

    // for class-level
    constructor(
        api: Int,
        sv: SignatureVisitor? = null
    ) : this(mutableListOf(), api, sv)

    val typeParameters: MutableList<FormalTypeParameter> = typeParams.toMutableList()

    private var returnType: TypeSignature? = null
    private val extends: MutableList<TypeSignature> = mutableListOf()

    private val methodParameters: MutableList<TypeSignature> = mutableListOf()

    val asClassSignatureNode: ClassSignatureNode
        get() = ClassSignatureNode(typeParameters.toList(), extends.toList())
    val asTypeSignatureNode: TypeSignatureNode
        get() = TypeSignatureNode(typeParameters.toList(), extends.toList())
    val asMethodSignatureNode: MethodSignatureNode
        get() = MethodSignatureNode(typeParameters.toList(), methodParameters.toList(), returnType!!)
    val asFieldSignatureNode: FieldSignatureNode
        get() = FieldSignatureNode(typeParameters.toList(), extends.first())

    override fun visitFormalTypeParameter(name: String) {
        typeParameters += FormalTypeParameter(name)
        sv?.visitFormalTypeParameter(name)
    }

    override fun visitClassBound(): SignatureVisitor {
        return FormalTypeParameterVisitor(
            typeParameters.last(),
            typeParameters::firstByName,
            api,
            sv?.visitClassBound()
        )
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        return FormalTypeParameterVisitor(
            typeParameters.last(),
            typeParameters::firstByName,
            api,
            sv?.visitInterfaceBound()
        )
    }

    override fun visitSuperclass(): SignatureVisitor {
        return TypeArgumentVisitor(
            onTypeArgument = extends::add,
            formalTypeParameters = typeParameters::firstByName,
            api = api,
            signatureVisitor = sv?.visitSuperclass()
        )
    }

    override fun visitInterface(): SignatureVisitor {
        return TypeArgumentVisitor(
            onTypeArgument = extends::add,
            formalTypeParameters = typeParameters::firstByName,
            api = api,
            signatureVisitor = sv?.visitInterface()
        )
    }

    override fun visitReturnType(): SignatureVisitor {
        return TypeArgumentVisitor(
            onTypeArgument = { returnType = it },
            formalTypeParameters = typeParameters::firstByName,
            api = api,
            signatureVisitor = sv?.visitReturnType()
        )
    }

    override fun visitParameterType(): SignatureVisitor {
        return TypeArgumentVisitor(
            onTypeArgument = methodParameters::add,
            formalTypeParameters = typeParameters::firstByName,
            api = api,
            signatureVisitor = sv?.visitParameterType()
        )
    }

}

internal fun MutableList<FormalTypeParameter>.firstByName(
    name: String
): FormalTypeParameter {
    return findBy(FormalTypeParameter::name, name)
        ?: FormalTypeParameter(name) // can be null; see GenerticsTest#problematic signature of reified function
}

class SignatureParsingException(message: String) : IllegalStateException(message)