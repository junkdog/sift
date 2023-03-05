package sift.core.dsl

import sift.core.api.*
import sift.core.element.AsmType
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
    override var action: Action.Chain<IterClasses> = chainFrom(Action.Class.ClassScope)
) : Core<ClassNode>(), CommonOperations<ClassNode, Classes>,
    ParentOperations<ClassNode, Classes>
{
    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Class.Filter(regex, invert)
    }

    /** Filters the currently inspected class nodes by checking if they implement a particular type. */
    fun implements(type: AsmType) {
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
        val scope = Classes().also(f).action
            .let { scope -> Action.Class.IntoInterfaces(recursive, synthesize) andThen scope }

        action += Action.Fork(scope)
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
        val forkTo = Action.Class.ReadType
            .let { Action.Fork(it) }

        action +=  forkTo

        return forkTo.forked
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
        val forkTo = Methods().also(f).action
            .let { methodScope -> Action.Class.IntoMethods andThen methodScope }

        action += Action.Fork(forkTo)
    }

    fun fields(f: Fields.() -> Unit) {
        val forkTo = Fields().also(f).action
            .let { fieldScope -> Action.Class.IntoFields andThen fieldScope }

        action += Action.Fork(forkTo)
    }

    fun superclass(f: Signature.() -> Unit) {
        val forkTo = Signature().also(f).action
            .let { signatureScope -> Action.Class.IntoSuperclassSignature andThen signatureScope }

        action += Action.Fork(forkTo)
    }
}