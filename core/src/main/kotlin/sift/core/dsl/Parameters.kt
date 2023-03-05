package sift.core.dsl

import sift.core.api.*
import sift.core.element.AsmType
import sift.core.element.MethodNode
import sift.core.element.ParameterNode
import sift.core.entity.Entity

/**
 * Parameters scope.
 *
 * @see [Methods.parameters]
 */
@SiftTemplateDsl
class Parameters internal constructor(
    parameters: Action<Iter<ParameterNode>, Iter<ParameterNode>> = Action.Parameter.ParameterScope
) : Core<ParameterNode>(),
    CommonOperations<ParameterNode, Parameters>,
    ParentOperations<MethodNode, Methods>
{

    override var action: Action.Chain<IterParameters> = chainFrom(parameters)

    fun parameter(nth: Int) {
        action += Action.Parameter.FilterNth(nth)
    }

    fun signature(f: Signature.() -> Unit) {
        val forkTo = Signature().also(f).action
            .let { signatureScope -> Action.Parameter.IntoSignature andThen signatureScope }

        action += Action.Fork(forkTo)
    }

    /**
     * Filters fields to include only those with a type matching type.
     */
    fun filterType(type: AsmType) {
        action += Action.Parameter.FilterType(type)
    }

    override fun scope(
        @Suppress("UNUSED_PARAMETER") label: String,
        f: Parameters.() -> Unit
    ) {
        val forkTo = Parameters().also(f).action
        action += Action.Fork(forkTo)
    }

    override fun scope(
        label: String,
        op: ScopeEntityPredicate,
        entity: Entity.Type,
        f: Parameters.() -> Unit
    ) {
        val forkTo = Parameters().also(f).action
        action += Action.ForkOnEntityExistence(forkTo, entity, op == ScopeEntityPredicate.ifExistsNot)
    }

    override fun outerScope(label: String, f: Methods.() -> Unit) {
        val forkTo = Action.Parameter.IntoOuterScope andThen Methods().also(f).action
        action += Action.Fork(forkTo)
    }

    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Parameter.Filter(regex, invert)
    }

    fun readType(): Action<IterParameters, IterValues> {
        val forkTo = Action.Parameter.ReadType
            .let { Action.Fork(it) }

        action += forkTo

        return forkTo.forked
    }

    fun explodeType(synthesize: Boolean = false, f: Classes.() -> Unit) {
        val explodeType = Action.Parameter.ExplodeType(synthesize)
        val forkTo = Classes().also(f).action

        action += Action.Fork(explodeType andThen forkTo)
    }
}