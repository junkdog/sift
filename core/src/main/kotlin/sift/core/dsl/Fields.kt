package sift.core.dsl

import sift.core.api.*
import sift.core.element.ClassNode
import sift.core.element.FieldNode
import sift.core.entity.Entity

/**
 * Fields scope.
 *
 * @see [Classes.fields]
 * @see [Template.fieldsOf]
 */
@SiftTemplateDsl
class Fields internal constructor(
    fields: Action<Iter<FieldNode>, Iter<FieldNode>> = Action.Field.FieldScope
) : Core<FieldNode>(chainFrom(fields), AccessFlags.Scope.Field),
    CommonOperations<FieldNode, Fields>,
    ParentOperations<ClassNode, Classes>
{
    override fun scope(
        @Suppress("UNUSED_PARAMETER") label: String,
        f: Fields.() -> Unit
    ) {
        val forkTo = Fields().also(f).action
        action += Action.Fork(forkTo)
    }

    override fun scope(
        label: String,
        op: ScopeEntityPredicate,
        entity: Entity.Type,
        f: Fields.() -> Unit
    ) {
        val forkTo = Fields().also(f).action
        action += Action.ForkOnEntityExistence(forkTo, entity, op == ScopeEntityPredicate.ifExistsNot)
    }

    override fun outerScope(label: String, f: Classes.() -> Unit) {
        action += Action.Fork(
            Action.Field.IntoOuterScope andThen Classes().also(f).action
        )
    }

    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Field.Filter(regex, invert)
    }

    /**
     * Filters fields to include only those with a type matching type.
     */
    fun filterType(type: Type) {
        action += Action.Field.FilterType(type.asmType)
    }

    fun explodeType(synthesize: Boolean = false, f: Classes.() -> Unit) {
        val explodeType = Action.Field.ExplodeType(synthesize)
        val forkTo = Classes().also(f).action

        action += Action.Fork(explodeType andThen forkTo)
    }

    fun signature(f: Signature.() -> Unit) {
        val forkTo = Signature().also(f).action
        action += Action.Fork(Action.Field.IntoSignature andThen forkTo)
    }
}