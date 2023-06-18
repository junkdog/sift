package sift.core.dsl

import sift.core.api.AccessFlags
import sift.core.api.AccessFlags.*
import sift.core.api.Action
import sift.core.api.Iter
import sift.core.element.Element

/** Element visiblity modifier. Note that [Internal] corresponds to kotlin's `internal` modifier. */
enum class Visibility {
    Private,
    PackagePrivate,
    Protected,
    Internal,
    Public;

    companion object {
        internal fun from(access: Int): Visibility {
            val visiblityBitmask = AccessFlags.bitmaskOf(acc_public, acc_protected, acc_private)
            return when (access and visiblityBitmask) {
                acc_public.flag    -> Public
                acc_protected.flag -> Protected
                acc_private.flag   -> Private
                else               -> PackagePrivate
            }
        }
    }
}

interface FilterableByVisibility<ELEMENT : Element> {
    /** filter elements by visibility */
    fun filter(
        access: Visibility,
        invert: Boolean = false
    )

    companion object {
        internal fun <ELEMENT : Element> scopedTo(
            action: Action.Chain<Iter<ELEMENT>>,
        ): FilterableByVisibility<ELEMENT> = FilterableByVisibilityImpl(action)
    }
}

private class FilterableByVisibilityImpl<ELEMENT : Element>(
    val action: Action.Chain<Iter<ELEMENT>>,
) : FilterableByVisibility<ELEMENT> {
    override fun filter(
        access: Visibility,
        invert: Boolean
    ) {
        action += Action.FilterVisibility(access)
    }
}