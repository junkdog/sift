package sift.core.dsl

import sift.core.api.AccessFlags
import sift.core.api.Action
import sift.core.api.Iter
import sift.core.element.Element

interface FilterableByAccessFlag<ELEMENT : Element> {
    /** filter elements by access flags */
    fun filter(
        modifiers: List<AccessFlags>,
        invert: Boolean = false
    )

    /** filter elements by access modifiers */
    fun filter(
        vararg modifiers: AccessFlags,
        invert: Boolean = false
    ) {
        filter(modifiers.toList(), invert)
    }

    companion object {
        internal fun <ELEMENT : Element> from(
            action: Action.Chain<Iter<ELEMENT>>,
            scope: AccessFlags.Scope
        ): FilterableByAccessFlag<ELEMENT> = FilterableByAccessFlagImpl(action, scope)
    }
}

private class FilterableByAccessFlagImpl<ELEMENT: Element>(
    val action: Action.Chain<Iter<ELEMENT>>,
    val scope: AccessFlags.Scope
) : FilterableByAccessFlag<ELEMENT> {
    override fun filter(
        modifiers: List<AccessFlags>,
        invert: Boolean
    ) {
        if (scope != AccessFlags.Scope.TypeErased) {
            modifiers
                .filter { scope !in it.scopes }
                .map { it.name.lowercase() }
                .takeIf(List<String>::isNotEmpty)
                ?.let { error("Modifiers $it are not applicable in ${scope.name.lowercase()} scope") }
        }

        action += Action.FilterModifiers(AccessFlags.bitmaskOf(modifiers), invert)
    }
}