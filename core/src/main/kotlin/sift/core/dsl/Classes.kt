package sift.core.dsl

import sift.core.api.*
import sift.core.element.ClassNode
import sift.core.entity.Entity

/**
 * Classes scope.
 *
 * @see [Template.classes]
 * @see [Template.classesOf]
 */
@SiftTemplateDsl
class Classes internal constructor(
    source: IsoAction<ClassNode> = Action.Class.ClassScope
) : Core<ClassNode>(chainFrom(source)),
    CommonOperations<ClassNode, Classes>,
    ParentOperations<ClassNode, Classes>
{
    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Class.Filter(regex, invert)
    }

    /** Filters the currently inspected class nodes by checking if they implement a particular type. */
    fun implements(type: Type) {
        action += Action.Class.FilterImplemented(type)
    }

    fun enums(f: Fields.() -> Unit) {
        action += Action.Fork(
            Action.Class.IntoEnumValues andThen Fields().also(f).action
        )
    }

    /**
     * Iterates the interfaces of current class nodes. Includes interfaces of super class.
     * Includes interfaces from all ancestors if [recursive] is `true`.
     */
    fun interfaces(recursive: Boolean = false, synthesize: Boolean = false, f: Classes.() -> Unit) {
        val intoInterfaces = Action.Class.IntoInterfaces(recursive, synthesize)
        action += Action.Fork(intoInterfaces andThen Classes().also(f).action)
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

    fun readType(): Action<IterClasses, IterValues> {
        return Action.Class.ReadType
    }

    override fun scope(
        @Suppress("UNUSED_PARAMETER") label: String,
        f: Classes.() -> Unit
    ) {
        val forkTo = Classes().also(f).action
        action += Action.Fork(forkTo)
    }

    override fun scope(
        label: String,
        op: ScopeEntityPredicate,
        entity: Entity.Type,
        f: Classes.() -> Unit
    ) {
        val forkTo = Classes().also(f).action
        action += Action.ForkOnEntityExistence(forkTo, entity, op == ScopeEntityPredicate.ifExistsNot)
    }

    /** iterates any outer classes */
    override fun outerScope(
        label: String,
        f: Classes.() -> Unit
    ) {
        val forkTo = Action.Class.IntoOuterClass andThen Classes().also(f).action
        action += Action.Fork(forkTo)
    }

    fun methods(f: Methods.() -> Unit) {
        val forkTo = Action.Class.IntoMethods andThen Methods().also(f).action
        action += Action.Fork(forkTo)
    }

    fun fields(f: Fields.() -> Unit) {
        val forkTo = Action.Class.IntoFields andThen Fields().also(f).action
        action += Action.Fork(forkTo)
    }

    fun superclass(f: Signature.() -> Unit) {
        val forkTo = Action.Class.IntoSuperclassSignature andThen Signature().also(f).action
        action += Action.Fork(forkTo)
    }
}