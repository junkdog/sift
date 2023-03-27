package sift.core.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.api.IterValues
import sift.core.element.Element
import sift.core.entity.Entity
import sift.core.entity.LabelFormatter
import sift.core.terminal.StringEditor
import sift.core.terminal.TextTransformer
import kotlin.reflect.KProperty1

abstract class Core<ELEMENT : Element> {

    internal abstract var action: Action.Chain<Iter<ELEMENT>>

    data class Property<T: Element>(
        val key: String,
        internal val action: Action<Iter<T>, IterValues>,
    )

    private var stack: MutableList<Action<*, *>> = mutableListOf()

    /**
     * Set the [entity] label from a [pattern], replacing any `${}` variables
     * inside [pattern] with values from [Entity.properties].
     *
     * ## Example: jdbi3
     *
     * Entity labels from SQL inside `@SqlUpdate` annotation
     *
     * ```
     * methods {
     *     annotatedBy(A.sqlUpdate)
     *     entity(E.sqlUpdate, label("\${sql}"),
     *         property("sql", readAnnotation(A.sqlUpdate, "value"))
     *     )
     * }
     * ```
     */
    fun label(
        pattern: String,
        vararg ops: TextTransformer
    ) = LabelFormatter.FromPattern(pattern, ops.toList())


    /**
     * Filters elements that belongs to [entity].
     */
    fun filter(entity: Entity.Type) {
        action += Action.EntityFilter(entity)
    }

    /** new entity with label from [labelFormatter] */
    fun entity(
        id: Entity.Type,
        labelFormatter: LabelFormatter,
        errorIfExists: Boolean = true,
        vararg properties: Property<ELEMENT>
    ) {
        action += Action.RegisterEntity(id, errorIfExists, labelFormatter)

        if (properties.isNotEmpty()) {
            action += properties
                .mapNotNull(Property<ELEMENT>::action)
                .map { Action.Fork(it) as Action<Iter<ELEMENT>, Iter<ELEMENT>> }
                .reduce { acc, action -> acc andThen action }
        }
    }

    /** register entity */
    fun entity(
        id: Entity.Type,
        vararg properties: Property<ELEMENT>
    ) {
        entity(id, LabelFormatter.FromElement, true, *properties)
    }

    /** register entity */
    fun entity(
        id: Entity.Type,
        labelFormatter: LabelFormatter,
        vararg properties: Property<ELEMENT>
    ) {
        entity(id, labelFormatter, true, *properties)
    }

    /** new entity with label inferred from introspected bytecode element */
    fun entity(
        id: Entity.Type,
        errorIfExists: Boolean = true,
        vararg properties: Property<ELEMENT>
    ) {
        entity(id, LabelFormatter.FromElement, errorIfExists, *properties)
    }

    /** updates existing entities with property */
    fun property(
        entity: Entity.Type,
        key: String,
        extract: Action<Iter<ELEMENT>, IterValues>
    ) {
        action += Action.Fork(
            extract andThen Action.UpdateEntityProperty(key, entity)
        )
    }

    /**
     * When `--debug` is past to the CLI, prints [tag] and all elements
     * currently in scope.
     *
     * Note that for most use-cases, `--profile` yields better results
     * without having to modify the template.
     **/
    fun log(tag: String) {
        action += Action.DebugLog(tag)
    }

    /**
     * When `--debug` is past to the CLI, prints [tag] and the count
     * of elements currently in scope.
     *
     * Note that for most use-cases, `--profile` yields better results
     * without having to modify the template.
     **/
    fun logCount(tag: String) {
        action += Action.DebugLog(tag, format = Action.DebugLog.LogFormat.Count)
    }

    /**
     * Associates [value] with entity.
     *
     * ## Example
     * ```
     * annotatedBy(A.XmlController)
     * property(SE.controller, "@style-as", withValue(E.XmlController))
     * ```
     */
    fun withValue(value: Entity.Type) = withErasedValue(value)
    fun withValue(value: Number) = withErasedValue(value)
    fun withValue(value: Boolean) = withErasedValue(value)
    fun withValue(value: String) = withErasedValue(value)
    fun <E> withValue(value: E) where E : Enum<E> = withErasedValue(value)

    @Suppress("UNCHECKED_CAST")
    private fun withErasedValue(
        value: Any
    ): Action<Iter<ELEMENT>, IterValues> = Action.WithValue(value)

    fun editText(
        vararg ops: TextTransformer
    ): Action<Iter<ELEMENT>, IterValues> = Action.EditText(StringEditor(ops.toList()))

    /**
     * Reads the short form name of the element
     */
    fun readName(
        shorten: Boolean = false
    ): Action<Iter<ELEMENT>, IterValues> = Action.ReadName(shorten)

    inline fun <reified T> annotatedBy() = annotatedBy(type<T>())

    fun annotatedBy(annotation: Type) {
        action += Action.HasAnnotation(annotation.asmType)
    }

    inline fun <reified T : Annotation> readAnnotation(
        field: KProperty1<T, *>
    ): Action<Iter<ELEMENT>, IterValues> = readAnnotation(type<T>(), field.name)

    fun readAnnotation(
        annotation: Type,
        field: String
    ): Action<Iter<ELEMENT>, IterValues> = Action.ReadAnnotation(annotation.asmType, field)

    /**
     * This entity tracks [children] under the label denoted by [key].
     *
     * ## Example
     * ```
     * classes {
     *     filter(Regex("^sift\\.core\\.api\\.testdata"))
     *     annotatedBy<RestController>()
     *     entity(controller)
     *     methods {
     *         annotatedBy<Endpoint>()
     *         entity(endpoint, label("\${http-method} \${path}"),
     *             property("http-method", readAnnotation(Endpoint::method)),
     *             property("path", readAnnotation(Endpoint::path)))
     *
     *         // relating endpoints to controllers
     *         controller["endpoints"] = endpoint
     *     }
     * }
     * ```
     */
    operator fun Entity.Type.set(
        key: String,
        children: Entity.Type
    ) {
        action += Action.RegisterChildren(this, key, children)
    }

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
    ): Property<ELEMENT> {
        return Property(tag, extract andThen Action.UpdateEntityProperty(tag))
    }
}
