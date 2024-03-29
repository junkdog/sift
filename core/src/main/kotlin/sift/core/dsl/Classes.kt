package sift.core.dsl

import sift.core.api.*
import sift.core.element.ClassNode
import sift.core.entity.Entity
import kotlin.reflect.KProperty1

/**
 * Classes scope.
 *
 * @see [Template.classes]
 * @see [Template.classesOf]
 */
@SiftTemplateDsl
class Classes internal constructor(
    source: IsoAction<ClassNode> = Action.Class.ClassScope,
    action: Action.Chain<Iter<ClassNode>> = chainFrom(source)
) : Core<ClassNode>(action),
    Annotatable<ClassNode>            by Annotatable.scopedTo(action),
    FilterableByAccessFlag<ClassNode> by FilterableByAccessFlag.scopedTo(action, AccessFlags.Scope.Class),
    FilterableByVisibility<ClassNode> by FilterableByVisibility.scopedTo(action),
    FilterableByType<ClassNode>       by FilterableByType.scopedTo(action),
    CommonOperations<ClassNode, Classes>,
    ParentOperations<ClassNode, Classes>
{
    override fun filter(regex: Regex, invert: Boolean) {
        action += Action.Class.Filter(regex, invert)
    }

    /** Filters the currently inspected class nodes by checking if they implement a particular type. */
    fun implements(type: SiftType) {
        action += Action.Class.FilterImplemented(type)
    }

    fun enums(f: Fields.() -> Unit) {
        enums(null, f)
    }

    fun enums(label: String?, f: Fields.() -> Unit) {
        action += Action.Fork(label,
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

    fun readType(): Action<IterClasses, IterValues> {
        return Action.Class.ReadType
    }

    override fun scope(
        label: String,
        f: Classes.() -> Unit
    ) {
        val forkTo = Classes().also(f).action
        action += Action.Fork(label.takeIf(String::isNotEmpty), forkTo)
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
        action += Action.Fork(label, forkTo)
    }

    /**
     * iterates methods of current classes. When [inherited] is `true`,
     * fields inherited from super classes are included too.
     */
    @Deprecated("Use methods(selection: MethodSelection, f: Methods.() -> Unit) instead")
    fun methods(
        inherited: Boolean,
        f: Methods.() -> Unit
    ) {
        val selection = MethodSelection.inherited.takeIf { inherited }
            ?: MethodSelection.declared

        methods(selection, f)
    }

    fun methods(
        selection: MethodSelectionFilter = MethodSelection.declared,
        f: Methods.() -> Unit
    ) {
        methods(null, selection, f)
    }

    fun methods(
        label: String?,
        selection: MethodSelectionFilter = MethodSelection.declared,
        f: Methods.() -> Unit
    ) {
        val methodFilter = (selection as? MethodSelectionSet)
            ?: MethodSelectionSet(setOf(selection as MethodSelection))

        val forkTo = Action.Class.IntoMethods(methodFilter) andThen Methods().also(f).action
        action += Action.Fork(label, forkTo)
    }

    /**
     * iterates fields of current classes. When [inherited] is `true`,
     * fields inherited from super classes are included too.
     */
    fun fields(inherited: Boolean = false, f: Fields.() -> Unit) {
        fields(null, inherited, f)
    }

    fun fields(label: String?, inherited: Boolean = false, f: Fields.() -> Unit) {
        val forkTo = Action.Class.IntoFields(inherited) andThen Fields().also(f).action
        action += Action.Fork(label, forkTo)
    }

    fun superclass(f: Signature.() -> Unit) {
        val forkTo = Action.Class.IntoSuperclassSignature andThen Signature().also(f).action
        action += Action.Fork(forkTo)
    }

    inline fun <reified T : Annotation> readAnnotation(
        field: KProperty1<T, *>
    ): Action<IterClasses, IterValues> = readAnnotation(type<T>(), field.name)
}
