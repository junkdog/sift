package sift.core.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.api.IterValues
import sift.core.element.Element
import sift.core.entity.Entity
import sift.core.terminal.StringEditor
import sift.core.terminal.TextTransformer

interface EntityPropertyRegistrar<ELEMENT : Element> {
    /** updates existing entities with property */
    fun property(
        entity: Entity.Type,
        key: String,
        extract: Action<Iter<ELEMENT>, IterValues>
    )

    /**
     * Associates [value] with entity.
     *
     * ## Example
     * ```
     * annotatedBy(A.XmlController)
     * property(SE.controller, "@style-as", withValue(E.XmlController))
     * ```
     */
    fun withValue(value: Entity.Type): Action<Iter<ELEMENT>, IterValues>
    fun withValue(value: Number): Action<Iter<ELEMENT>, IterValues>
    fun withValue(value: Boolean): Action<Iter<ELEMENT>, IterValues>
    fun withValue(value: String): Action<Iter<ELEMENT>, IterValues>
    fun <E> withValue(value: E): Action<Iter<ELEMENT>, IterValues> where E : Enum<E>

    @Suppress("UNCHECKED_CAST")
    fun withErasedValue(
        value: Any
    ): Action<Iter<ELEMENT>, IterValues>

    fun editText(
        vararg ops: TextTransformer
    ): Action<Iter<ELEMENT>, IterValues>

    /**
     * Reads the short form name of the element
     */
    fun readName(
        shorten: Boolean = false
    ): Action<Iter<ELEMENT>, IterValues>

    fun annotatedBy(annotation: Type)

    fun readAnnotation(
        annotation: Type,
        field: String
    ): Action<Iter<ELEMENT>, IterValues>

    /**
     * Associates entity property [tag] with result of [extract] action.
     *
     * ## Example
     * ```
     * entity(endpoint, label("\${http-method} \${path}"),
     *     property("http-method", readAnnotation(Endpoint::method)),
     *     property("path", readAnnotation(Endpoint::path)))
     * ```
     */
    fun property(
        tag: String,
        extract: Action<Iter<ELEMENT>, IterValues>
    ): Property<ELEMENT>

    companion object {
        fun <ELEMENT: Element> scopedTo(
            action: Action.Chain<Iter<ELEMENT>>
        ): EntityPropertyRegistrar<ELEMENT> = EntityPropertyRegistrarImpl(action)
    }
}

private class EntityPropertyRegistrarImpl<ELEMENT : Element>(
    val action: Action.Chain<Iter<ELEMENT>>
) : EntityPropertyRegistrar<ELEMENT> {

    override fun property(
        entity: Entity.Type,
        key: String,
        extract: Action<Iter<ELEMENT>, IterValues>
    ) {
        action += Action.Fork(
            extract andThen Action.UpdateEntityProperty(key, entity)
        )
    }

    override fun withValue(value: Entity.Type) = withErasedValue(value)
    override fun withValue(value: Number) = withErasedValue(value)
    override fun withValue(value: Boolean) = withErasedValue(value)
    override fun withValue(value: String) = withErasedValue(value)
    override fun <E> withValue(value: E) where E : Enum<E> = withErasedValue(value)

    @Suppress("UNCHECKED_CAST")
    override fun withErasedValue(
        value: Any
    ): Action<Iter<ELEMENT>, IterValues> = Action.WithValue(value)

    override fun editText(
        vararg ops: TextTransformer
    ): Action<Iter<ELEMENT>, IterValues> = Action.EditText(StringEditor(ops.toList()))

    override fun readName(
        shorten: Boolean
    ): Action<Iter<ELEMENT>, IterValues> = Action.ReadName(shorten)


    override fun annotatedBy(annotation: Type) {
        action += Action.HasAnnotation(annotation.asmType)
    }

    override fun readAnnotation(
        annotation: Type,
        field: String
    ): Action<Iter<ELEMENT>, IterValues> = Action.ReadAnnotation(annotation.asmType, field)

    override fun property(
        tag: String,
        extract: Action<Iter<ELEMENT>, IterValues>
    ): Property<ELEMENT> {
        return Property(tag, extract andThen Action.UpdateEntityProperty(tag))
    }
}

data class Property<T: Element>(
    val key: String,
    internal val action: Action<Iter<T>, IterValues>,
)
