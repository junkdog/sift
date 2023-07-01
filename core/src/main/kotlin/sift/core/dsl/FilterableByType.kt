package sift.core.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.element.Element
import sift.core.element.Trait

interface FilterableByType<ELEMENT>
    where ELEMENT : Element,
          ELEMENT : Trait.HasType
{
    /**
     * Filters elements to include only those representing a matching type.
     */
    fun filterType(type: SiftType)

    companion object {
        internal fun <ELEMENT> scopedTo(
            action: Action.Chain<Iter<ELEMENT>>,
        ): FilterableByType<ELEMENT> where ELEMENT : Element, ELEMENT : Trait.HasType {
            return FilterableByTypeImpl(action)
        }
    }
}

private class FilterableByTypeImpl<ELEMENT>(
    val action: Action.Chain<Iter<ELEMENT>>,
) : FilterableByType<ELEMENT>
    where ELEMENT : Element,
          ELEMENT : Trait.HasType
{
    override fun filterType(type: SiftType) {
        action += Action.FilterType(type)
    }
}