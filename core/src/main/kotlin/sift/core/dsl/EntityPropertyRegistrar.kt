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
        strategy: PropertyStrategy,
        extract: Action<Iter<ELEMENT>, IterValues>
    )

    fun property(
        entity: Entity.Type,
        key: String,
        extract: Action<Iter<ELEMENT>, IterValues>
    ) = property(entity, key, PropertyStrategy.replace, extract)

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
        key: String,
        strategy: PropertyStrategy,
        extract: Action<Iter<ELEMENT>, IterValues>
    ): Property<ELEMENT>

    fun property(
        key: String,
        extract: Action<Iter<ELEMENT>, IterValues>
    ): Property<ELEMENT> = property(key, PropertyStrategy.replace, extract)

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

    /** internal function primarily used during graphviz dot generation */
    fun editor(vararg ops: TextTransformer): Action<Iter<ELEMENT>, IterValues>

    /**
     * Reads the short form name of the element
     */
    fun readName(
        shorten: Boolean = false
    ): Action<Iter<ELEMENT>, IterValues>

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
        strategy: PropertyStrategy,
        extract: Action<Iter<ELEMENT>, IterValues>
    ) {
        action += Action.Fork(
            extract andThen Action.UpdateEntityProperty(strategy, key, entity)
        )
    }

    override fun withValue(value: Entity.Type) = withErasedValue(value)
    override fun withValue(value: Number) = withErasedValue(value)
    override fun withValue(value: Boolean) = withErasedValue(value)
    override fun withValue(value: String) = withErasedValue(value)
    override fun <E> withValue(value: E) where E : Enum<E> = withErasedValue(value)

    fun withErasedValue(
        value: Any
    ): Action<Iter<ELEMENT>, IterValues> = Action.WithValue(value)

    override fun editor(
        vararg ops: TextTransformer
    ): Action<Iter<ELEMENT>, IterValues> = Action.Editor(StringEditor(ops.toList()))

    override fun readName(
        shorten: Boolean
    ): Action<Iter<ELEMENT>, IterValues> = Action.ReadName(shorten)

    override fun property(
        key: String,
        strategy: PropertyStrategy,
        extract: Action<Iter<ELEMENT>, IterValues>
    ): Property<ELEMENT> {
        return Property(key, extract andThen Action.UpdateEntityProperty(strategy, key))
    }
}

/**
 * Property update strategy comes into play when the property already exists.
 */
@Suppress("EnumEntryName")
enum class PropertyStrategy {
    /** replaces existing value */
    replace,
    /** appends to existing value */
    append,
    /** prepends to existing value */
    prepend,
    /** does not update existing value */
    immutable,
    /** appends value unless it exists */
    unique
}

data class Property<T: Element>(
    val key: String,
    internal val action: Action<Iter<T>, IterValues>,
)

/** composes a new action by applying [transformer] to the value of this element */
infix fun <ELEMENT : Element> Action<Iter<ELEMENT>, IterValues>.andThen(
    transformer: TextTransformer
): Action<Iter<ELEMENT>, IterValues> {
    return this andThen Action.EditText(StringEditor(listOf(transformer)))
}