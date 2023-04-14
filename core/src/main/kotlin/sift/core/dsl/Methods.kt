package sift.core.dsl

import sift.core.api.*
import sift.core.element.ClassNode
import sift.core.element.MethodNode
import sift.core.entity.Entity

/**
 * Methods scope.
 *
 * @see [Classes.methods]
 * @see [Template.methodsOf]
 */
@SiftTemplateDsl
class Methods internal constructor(
    methods: Action<Iter<MethodNode>, Iter<MethodNode>> = Action.Method.MethodScope
) : Core<MethodNode>(chainFrom(methods), AccessFlags.Scope.Method),
    CommonOperations<MethodNode, Methods>,
    ParentOperations<ClassNode, Classes>
{
    operator fun Entity.Type.set(
        key: String,
        rhs: EntityResolution
    ) {
        val resolver = when (rhs) {
            is Instantiations -> EntityAssignmentResolver.FromInstantiationsOf(key, rhs.type)
            is Invocations -> EntityAssignmentResolver.FromInvocationsOf(key, rhs.type)
            is FieldAccess -> EntityAssignmentResolver.FromFieldAccessOf(key, rhs.type)
        }

        action += Action.RegisterChildrenFromResolver(this, key, resolver)
    }

    operator fun EntityResolution.set(
        key: String,
        rhs: Entity.Type
    ) {
        val resolver = when (this) {
            is Instantiations -> EntityAssignmentResolver.FromInstantiationsBy(key, type)
            is Invocations -> EntityAssignmentResolver.FromInvocationsBy(key, type)
            is FieldAccess -> EntityAssignmentResolver.FromFieldAccessBy(key, type)
        }

        action += Action.RegisterChildrenFromResolver(rhs, key, resolver)
    }

    override fun scope(
        @Suppress("UNUSED_PARAMETER") label: String,
        f: Methods.() -> Unit
    ) {
        val forkTo = Methods().also(f).action
        action += Action.Fork(forkTo)
    }

    override fun scope(
        label: String,
        op: ScopeEntityPredicate,
        entity: Entity.Type,
        f: Methods.() -> Unit
    ) {
        val forkTo = Methods().also(f).action
        action += Action.ForkOnEntityExistence(forkTo, entity, op == ScopeEntityPredicate.ifExistsNot)
    }

    override fun outerScope(
        label: String,
        f: Classes.() -> Unit
    ) {
        val forkTo = Action.Method.IntoOuterScope andThen Classes().also(f).action
        action += Action.Fork(forkTo)
    }

    /** filter elements by name and owner  */
    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Method.Filter(regex, invert)
    }

    fun filterName(regex: Regex, invert: Boolean = false) {
        action += Action.Method.FilterName(regex, invert)
    }

    fun filterName(name: String, invert: Boolean = false) {
        filterName(Regex.fromLiteral(name), invert)
    }

    fun declaredMethods() {
        action += Action.Method.DeclaredMethods
    }

    fun parameters(f: Parameters.() -> Unit) {
        val forkTo = Parameters().also(f).action

        action += Action.Fork(
            Action.Method.IntoParameters andThen forkTo
        )
    }

    /** iterate fields accessed by current methods */
    fun fieldAccess(
        f: Fields.() -> Unit
    ) {
        val fieldsScope = Fields().also(f).action

        action += Action.Fork(
            Action.Method.FieldAccess andThen fieldsScope
        )
    }

    fun instantiationsOf(type: Entity.Type, f: Classes.() -> Unit) {
        val classScope = Classes().also(f).action

        action += Action.Fork(
            Action.Method.Instantiations(type) andThen classScope
        )
    }

    fun invocationsOf(
        type: Entity.Type,
        synthesize: Boolean = false,
        f: Methods.() -> Unit
    ) {
        val methodsScope = Methods(Action.Method.InvocationsOf(type, synthesize))
            .also(f)
            .action

        action += Action.Fork(methodsScope)
    }

    fun invokes(
        type: Entity.Type,
    ) {
        action += Action.Method.Invokes(type)
    }

    fun returns(f: Signature.() -> Unit) {
        action += Action.Fork(
            Action.Method.IntoReturnSignature andThen Signature().also(f).action
        )
    }

    val Entity.Type.instantiations: EntityResolution
        get() = Instantiations(this)
    val Entity.Type.invocations: EntityResolution
        get() = Invocations(this)
    val Entity.Type.fieldAccess: EntityResolution
        get() = FieldAccess(this)
}