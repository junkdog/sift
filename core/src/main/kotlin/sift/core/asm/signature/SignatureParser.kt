package sift.core.asm.signature

import net.onedaybeard.collectionsby.firstBy
import org.objectweb.asm.signature.SignatureVisitor


data class TypeSignatureNode(
    val parameters: List<FormalTypeParameter>,
    val extends: List<TypeSignature>
)

data class ClassSignatureNode(
    val parameters: List<FormalTypeParameter>,
    val extends: TypeSignature,
    val implements: List<TypeSignature>
) {
    constructor(
        parameters: List<FormalTypeParameter>,
        extends: List<TypeSignature>
    ) : this(parameters, extends.first(), extends.drop(1))
}

data class MethodSignatureNode(
    val parameters: List<FormalTypeParameter>,
    val methodParameters: List<TypeSignature>,
    val returnType: TypeSignature?,
)


class SignatureParser(
    typeParams: MutableList<FormalTypeParameter>,
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
        get() = MethodSignatureNode(typeParameters.toList(), methodParameters.toList(), returnType)

    override fun visitFormalTypeParameter(name: String) {
        typeParameters += FormalTypeParameter(name, null)
        sv?.visitFormalTypeParameter(name)
    }

    override fun visitClassBound(): SignatureVisitor {
        return FormalTypeParameterVisitor(
            typeParameters.last(),
            MetaType.Class,
            typeParameters::firstByName,
            typeParameters::add,
            api,
            sv?.visitClassBound()
        )
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        return FormalTypeParameterVisitor(
            typeParameters.last(),
            MetaType.Interface,
            typeParameters::firstByName,
            typeParameters::add,
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
) = firstBy(FormalTypeParameter::name, name)