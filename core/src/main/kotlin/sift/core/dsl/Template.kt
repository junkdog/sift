package sift.core.dsl

import sift.core.api.*
import sift.core.api.FieldAccess
import sift.core.api.Instantiations
import sift.core.api.Invocations
import sift.core.api.chainFrom
import sift.core.entity.Entity

@SiftTemplateDsl
class Template internal constructor(
    var action: Action.Chain<Unit> = chainFrom(Action.Template.TemplateScope)
) {

    /**
     * Stub missing classes and register them with entities.
     */
    fun synthesize(f: Synthesize.() -> Unit) {
        action += Synthesize().also(f).action
    }

    /**
     * Includes another template by copying it into this template.
     */
    fun include(template: Action<Unit, Unit>) {
        action += template
    }

    fun scope(label: String, f: Template.() -> Unit) {
        action += Template().also(f).action
    }

    fun scope(
        label: String,
        op: ScopeEntityPredicate,
        entity: Entity.Type,
        f: Template.() -> Unit
    ) {
        val forkTo = Template().also(f).action
        action += Action.ForkOnEntityExistence(forkTo, entity, op == ScopeEntityPredicate.ifExistsNot)
    }

    /** iterates all classes */
    fun classes(f: Classes.() -> Unit) {
        action += Classes()
            .also(f)
            .action
            .let(Action.Template.InstrumentClasses::andThen)
            .let { it andThen Action.Class.ToTemplateScope }
    }

    /** iterates class elements of registered [entity] type */
    fun classesOf(entity: Entity.Type, f: Classes.(Entity.Type) -> Unit) {
        val classes = Action.Template.ClassesOf(entity)
        val forkTo = Classes().apply { f(entity) }.action

        action += Action.Fork(classes andThen forkTo)
    }

    /** iterates method elements of registered [entity] type */
    fun methodsOf(entity: Entity.Type, f: Methods.(Entity.Type) -> Unit) {
        val methods = Action.Template.MethodsOf(entity)
        val forkTo = Methods().apply { f(entity) }.action

        action += Action.Fork(methods andThen forkTo)
    }

    /** iterates field elements of registered [entity] type */
    fun fieldsOf(entity: Entity.Type, f: Fields.(Entity.Type) -> Unit) {
        val fields = Action.Template.FieldsOf(entity)
        val forkTo = Fields().apply { f(entity) }.action

        action += Action.Fork(fields andThen forkTo)
    }

    /** iterates "scope-erased" elements, useful for property tagging entities. */
    fun elementsOf(entity: Entity.Type, f: Elements.(Entity.Type) -> Unit) {
        val elements = Action.Template.ElementsOf(entity)
        val forkTo = Elements().apply { f(entity) }.action

        action += Action.Fork(elements andThen forkTo)
    }

    /** associates all entities with [rhs] */
    operator fun Entity.Type.set(
        key: String,
        rhs: EntityResolution
    ) {
        methodsOf(this) { e ->
            e[key] = rhs
        }
    }

    val Entity.Type.instantiations: EntityResolution
        get() = Instantiations(this)
    val Entity.Type.invocations: EntityResolution
        get() = Invocations(this)
    val Entity.Type.fieldAccess: EntityResolution
        get() = FieldAccess(this)
}