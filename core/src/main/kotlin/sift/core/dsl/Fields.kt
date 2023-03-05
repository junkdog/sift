package sift.core.dsl

import sift.core.api.*
import sift.core.element.AsmType
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
) : Core<FieldNode>(),
    CommonOperations<FieldNode, Fields>,
    ParentOperations<ClassNode, Classes>
{

    override var action: Action.Chain<IterFields> = chainFrom(fields)

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
        val forkTo = Action.Field.IntoOuterScope andThen Classes().also(f).action
        action += Action.Fork(forkTo)
    }

    /** filter elements by access modifiers */
    fun filter(
        vararg modifiers: Modifiers,
        invert: Boolean = false
    ) {
        filter(modifiers.toList(), invert)
    }


    /** filter elements by access modifiers */
    fun filter(
        modifiers: List<Modifiers>,
        invert: Boolean = false
    ) {
        action += Action.FilterModifiers(Modifiers.bitmaskOf(modifiers), invert)
    }

    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Field.Filter(regex, invert)
    }

    /**
     * Filters fields to include only those with a type matching type.
     */
    fun filterType(type: AsmType) {
        action += Action.Field.FilterType(type)
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