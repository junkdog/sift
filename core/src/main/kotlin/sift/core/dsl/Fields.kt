package sift.core.dsl

import sift.core.api.*
import sift.core.element.ClassNode
import sift.core.element.FieldNode
import sift.core.entity.Entity
import kotlin.reflect.KProperty1

/**
 * Fields scope.
 *
 * @see [Classes.fields]
 * @see [Template.fieldsOf]
 */
@SiftTemplateDsl
class Fields internal constructor(
    fields: Action<Iter<FieldNode>, Iter<FieldNode>> = Action.Field.FieldScope,
    action: Action.Chain<Iter<FieldNode>> = chainFrom(fields),
) : Core<FieldNode>(action),
    Annotatable<FieldNode>            by Annotatable.scopedTo(action),
    FilterableByVisibility<FieldNode> by FilterableByVisibility.scopedTo(action),
    FilterableByAccessFlag<FieldNode> by FilterableByAccessFlag.scopedTo(action, AccessFlags.Scope.Field),
    FilterableByType<FieldNode>       by FilterableByType.scopedTo(action),
    CommonOperations<FieldNode, Fields>,
    ParentOperations<ClassNode, Classes>
{
    override fun scope(
        label: String,
        f: Fields.() -> Unit
    ) {
        val forkTo = Fields().also(f).action
        action += Action.Fork(label.takeIf(String::isNotEmpty), forkTo)
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
        action += Action.Fork(label,
            Action.Field.IntoOuterScope andThen Classes().also(f).action
        )
    }

    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Field.Filter(regex, invert)
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

    inline fun <reified T : Annotation> readAnnotation(
        field: KProperty1<T, *>
    ): Action<IterFields, IterValues> = readAnnotation(type<T>(), field.name)
}