package sift.core.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.api.IterValues
import sift.core.element.Element


interface Annotatable<ELEMENT : Element> {

    /**
     * Filter elements that are decorated by [annotation]
     */
    fun annotatedBy(annotation: SiftType)

    fun annotations(
        filter: SiftType? = null,
        f: Annotations.() -> Unit
    ) = annotations(null, filter, f)

    fun annotations(
        label: String? = null,
        filter: SiftType? = null,
        f: Annotations.() -> Unit
    )

    /**
     * Reads the element value of the [attribute] name belonging to [annotation]. If the element value
     * is an array, the array is flattened when added to entity properties.
     */
    fun readAnnotation(
        annotation: SiftType,
        attribute: String
    ): Action<Iter<ELEMENT>, IterValues>

    companion object {
        internal fun <ELEMENT : Element> scopedTo(
            action: Action.Chain<Iter<ELEMENT>>,
        ): Annotatable<ELEMENT> = AnnotatableImpl(action)
    }
}

private class AnnotatableImpl<ELEMENT : Element>(
    val action: Action.Chain<Iter<ELEMENT>>,
) : Annotatable<ELEMENT> {
    override fun annotatedBy(annotation: SiftType) {
        action += Action.HasAnnotation(annotation)
    }

    override fun annotations(
        label: String?,
        filter: SiftType?,
        f: Annotations.() -> Unit
    ) {
        action += Action.Fork(label, Action.IntoAnnotations<ELEMENT>(filter)
            andThen Annotations().also(f).action)
    }

    override fun readAnnotation(
        annotation: SiftType,
        attribute: String
    ): Action<Iter<ELEMENT>, IterValues> = Action.ReadAnnotation(annotation, attribute)
}

inline fun <reified T> Annotatable<*>.annotatedBy() {
    annotatedBy(type<T>())
}
