package sift.core.dsl

import sift.core.api.Action
import sift.core.element.Element
import sift.core.entity.Entity


interface ParentOperations<T : Element, PARENT_SCOPE : Core<T>> {
    fun outerScope(label: String, f: PARENT_SCOPE.() -> Unit)
}

interface CommonOperations<T : Element, SCOPE : Core<T>> {
    fun filter(regex: Regex, invert: Boolean = false)

    fun filter(string: String, invert: Boolean = false) {
        filter(Regex.fromLiteral(string), invert)
    }

    fun scope(label: String, f: SCOPE.() -> Unit)
    fun scope(
        label: String,
        op: ScopeEntityPredicate = ScopeEntityPredicate.ifExists,
        entity: Entity.Type,
        f: SCOPE.() -> Unit
    )
}

/** Define a new template */
fun template(f: Template.() -> Unit): Action<Unit, Unit> {
    return Template().also(f).action
}

/** Define a new template */
internal fun classes(f: Classes.() -> Unit): Action<Unit, Unit> {
    return template { classes(f) }
}


@Suppress("EnumEntryName")
enum class ScopeEntityPredicate {
    ifExists, ifExistsNot
}