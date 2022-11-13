package sift.core.api

import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import sift.core.*
import sift.core.api.Action.DebugLog.LogFormat
import sift.core.api.ScopeEntityPredicate.ifExistsNot
import sift.core.entity.Entity
import sift.core.entity.LabelFormatter
import sift.core.asm.type
import sift.core.jackson.NoArgConstructor
import java.util.*
import kotlin.reflect.KProperty1


@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class SiftTemplateDsl

@Suppress("UNCHECKED_CAST")
object Dsl {
    interface ParentOperations<T : Element, PARENT_SCOPE : Core<T>> {
        fun parentScope(label: String, f: PARENT_SCOPE.() -> Unit)
    }

    interface CommonOperations<T : Element, SCOPE : Core<T>> {
        fun filter(regex: Regex, invert: Boolean = false)

        /**
         * Filter elements that are decorated by [annotation]
         */
        fun annotatedBy(annotation: Type)
        fun readAnnotation(annotation: Type, field: String): Action<Iter<T>, IterValues>

        fun scope(label: String, f: SCOPE.() -> Unit)
        fun scope(
            label: String,
            op: ScopeEntityPredicate = ScopeEntityPredicate.ifExists,
            entity: Entity.Type,
            f: SCOPE.() -> Unit
        )

        fun property(tag: String, extract: Action<Iter<T>, IterValues>): Core.Property<T>
    }

    /** Define a new Instrumenter Pipeline */
    fun instrumenter(f: Instrumenter.() -> Unit): Action<Unit, Unit> {
        return Instrumenter()
            .also(f)
            .action
    }

    /** Define a new Instrumenter Pipeline */
    fun classes(f: Classes.() -> Unit): Action<Unit, Unit> {
        return Classes()
            .also(f)
            .action
            .let(Action.Instrumenter.InstrumentClasses::andThen)
            .let { it andThen Action.Class.ToInstrumenterScope }
    }

    @SiftTemplateDsl
    abstract class Core<ELEMENT : Element> {

        // hack: FIXME
        internal var currentProperty: Property<Element>? = null

        internal abstract var action: Action.Chain<Iter<ELEMENT>>

        data class Property<T: Element>(
            val key: String,
            val action: Action<Iter<T>, IterValues>?,
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
        fun label(pattern: String) = LabelFormatter.FromPattern(pattern)


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
         * without having to modify the pipeline.
         **/
        fun log(tag: String) {
            action += Action.DebugLog(tag)
        }

        /**
         * When `--debug` is past to the CLI, prints [tag] and the count
         * of elements currently in scope.
         *
         * Note that for most use-cases, `--profile` yields better results
         * without having to modify the pipeline.
         **/
        fun logCount(tag: String) {
            action += Action.DebugLog(tag, format = LogFormat.Count)
        }

        /**
         * Associates [value] with entity.
         *
         * ## Example
         * ```
         * annotatedBy(A.XmlController)
         * update(SE.controller, "@style-as", withValue(E.XmlController))
         * ```
         */
        fun withValue(value: Any): Action<Iter<ELEMENT>, IterValues> {
            val forkTo = Action.WithValue<ELEMENT>(value)
                .let { Action.Fork(it) }

            action += forkTo

            return forkTo.forked
        }

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

        internal inline fun <reified S: Core<T>, reified T : Element> scopedProperty(
            tag: String,
            scope: S,
            f: S.() -> Unit
        ): Property<T> {
            if (scope.currentProperty != null)
                Throw.publishOutsideOfProperty(action)

            // f() populates currentProperty.actioni
            scope.currentProperty  = Property(tag, null)
            scope.also(f)

            // dependent on executedScope; it is expected to publish() a value action
            val property = scope.currentProperty as Property<T>
            property.action ?: Throw.publishNeverCalled(tag)

            // clean up
            scope.currentProperty = null

            return property
        }
    }

    @SiftTemplateDsl
    class Synthesize(
        var action: Action.Chain<Unit> = chainFrom(Action.Instrumenter.InstrumenterScope)
    ) {
        /** Stub missing class node for [type] and register it to an entity */
        fun entity(
            id: Entity.Type,
            type: Type,
            labelFormatter: LabelFormatter = LabelFormatter.FromElement,
        ) {
            action += Action.RegisterSynthesizedEntity(id, type, labelFormatter)
        }

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
        fun label(pattern: String) = LabelFormatter.FromPattern(pattern)
    }

    @SiftTemplateDsl
    class Instrumenter(
        var action: Action.Chain<Unit> = chainFrom(Action.Instrumenter.InstrumenterScope)
    ) {

        /**
         * Stub missing classes and register them with entities.
         */
        fun synthesize(f: Synthesize.() -> Unit) {
            action += Synthesize().also(f).action
        }

        /**
         * Includes another instrumenter's pipeline by copying it into this pipeline.
         */
        fun include(pipeline: Action<Unit, Unit>) {
            action += pipeline
        }

        fun scope(label: String, f: Instrumenter.() -> Unit) {
            action += Instrumenter().also(f).action
        }

        fun scope(
            label: String,
            op: ScopeEntityPredicate,
            entity: Entity.Type,
            f: Instrumenter.() -> Unit
        ) {
            val forkTo = Instrumenter().also(f).action
            action += Action.ForkOnEntityExistence(forkTo, entity, op == ifExistsNot)
        }

        fun classes(f: Classes.() -> Unit) {
            action += Classes()
                .also(f)
                .action
                .let(Action.Instrumenter.InstrumentClasses::andThen)
                .let { it andThen Action.Class.ToInstrumenterScope }
        }

        /** iterates class elements of registered [entity] type */
        fun classesOf(entity: Entity.Type, f: Classes.() -> Unit) {
            val classes = Action.Instrumenter.ClassesOf(entity)
            val forkTo = Classes().also(f).action

            action += Action.Fork(classes andThen forkTo)
        }

        /** iterates method elements of registered [entity] type */
        fun methodsOf(entity: Entity.Type, f: Methods.() -> Unit) {
            val methods = Action.Instrumenter.MethodsOf(entity)
            val forkTo = Methods().also(f).action

            action += Action.Fork(methods andThen forkTo)
        }
    }

    class Classes(
        override var action: Action.Chain<IterClasses> = chainFrom(Action.Class.ClassScope)
    ) : Core<Element.Class>(), CommonOperations<Element.Class, Classes> {

        // utility

        inline fun <reified T> annotatedBy() = annotatedBy(type<T>())

        override fun annotatedBy(annotation: Type) {
            action += Action.HasAnnotation(annotation)
        }

        override fun filter(regex: Regex, invert: Boolean) {
            action += Action.Class.Filter(regex, invert)
        }

        fun implements(type: Type) {
            action += Action.Class.FilterImplemented(type)
        }

        override fun readAnnotation(
            annotation: Type,
            field: String
        ): Action<IterClasses, IterValues> {
            val forkTo = Action.ReadAnnotation<Element.Class>(annotation, field)
                .let { Action.Fork(it) }
            action +=  forkTo

            return forkTo.forked
        }

        inline fun <reified T : Annotation> readAnnotation(
            field: KProperty1<T, *>
        ): Action<IterClasses, IterValues> {
            return readAnnotation(type<T>(), field.name)
        }

        fun readType(): Action<IterClasses, IterValues> {
            val forkTo = Action.Class.ReadType
                .let { Action.Fork(it) }

            action +=  forkTo

            return forkTo.forked
        }

        fun readName(): Action<IterClasses, IterValues> {
            val forkTo = Action.Class.ReadName
                .let { Action.Fork(it) }

            action +=  forkTo

            return forkTo.forked
        }

        override fun scope(
            @Suppress("UNUSED_PARAMETER") label: String,
            f: Classes.() -> Unit
        ) {
            val forkTo = Classes().also(f).action
            action += Action.Fork(forkTo)
        }

        override fun scope(
            label: String,
            op: ScopeEntityPredicate,
            entity: Entity.Type,
            f: Classes.() -> Unit
        ) {
            val forkTo = Classes().also(f).action
            action += Action.ForkOnEntityExistence(forkTo, entity, op == ifExistsNot)
        }

        fun methods(f: Methods.() -> Unit) {
            val forkTo = Methods().also(f).action
                .let { methodScope -> Action.Class.IntoMethods andThen methodScope }

            action += Action.Fork(forkTo)
        }

        fun fields(f: Fields.() -> Unit) {
            val forkTo = Fields().also(f).action
                .let { fieldScope -> Action.Class.IntoFields andThen fieldScope }

            action += Action.Fork(forkTo)
        }
    }

    class Methods(
        methods: Action<Iter<Element.Method>, Iter<Element.Method>> = Action.Method.MethodScope
    ) : Core<Element.Method>(),
        CommonOperations<Element.Method, Methods>,
        ParentOperations<Element.Class, Classes>
    {

        override var action: Action.Chain<IterMethods> = chainFrom(methods)

        operator fun Entity.Type.set(
            key: String,
            children: EntityResolution
        ) {
            val resolver = when (children) {
                is EntityResolution.Instantiations -> EntityResolver.FromInstantiationsOf(key, children.type)
                is EntityResolution.Invocations -> EntityResolver.FromInvocationsOf(key, children.type)
            }

            action += Action.RegisterChildrenFromResolver(this, key, resolver)
        }

        override fun scope(
            @Suppress("UNUSED_PARAMETER") label: String,
            f: Methods.() -> Unit
        ) {
            val forkTo = Methods().also(f).action
            action += Action.Fork(forkTo)
        }

        override fun scope(
            label: String,
            op: ScopeEntityPredicate,
            entity: Entity.Type,
            f: Methods.() -> Unit
        ) {
            val forkTo = Methods().also(f).action
            action += Action.ForkOnEntityExistence(forkTo, entity, op == ifExistsNot)
        }

        override fun parentScope(
            label: String,
            f: Classes.() -> Unit
        ) {
            val forkTo = Action.Method.IntoParents andThen Classes().also(f).action
            action += Action.Fork(forkTo)
        }

        inline fun <reified T> annotatedBy() {
            annotatedBy(type<T>())
        }

        override fun filter(regex: Regex, invert: Boolean) {
            action += Action.Method.Filter(regex, invert)
        }

        fun declaredMethods() {
            action += Action.Method.DeclaredMethods
        }

        override fun annotatedBy(annotation: Type) {
            action += Action.HasAnnotation(annotation)
        }

        override fun readAnnotation(
            annotation: Type,
            field: String
        ): Action<IterMethods, IterValues> {
            val forkTo = Action.ReadAnnotation<Element.Method>(annotation, field)
                .let { Action.Fork(it) }

            action += forkTo

            return forkTo.forked
        }

        inline fun <reified T : Annotation> readAnnotation(
            field: KProperty1<T, *>
        ): Action<IterMethods, IterValues> = readAnnotation(type<T>(), field.name)

        fun parameters(f: Parameters.() -> Unit) {
            val forkTo = Parameters().also(f).action

            action += Action.Fork(
                Action.Method.IntoParameters andThen forkTo
            )
        }

        fun instantiationsOf(type: Entity.Type, f: Classes.() -> Unit) {
            val classScope = Classes().also(f).action

            action += Action.Fork(
                Action.Method.Instantiations(type) andThen classScope
            )
        }

        fun invocationsOf(
            type: Entity.Type,
            synthesize: Boolean = false,
            f: Methods.() -> Unit
        ) {
            val methodsScope = Methods(Action.Method.InvocationsOf(type, synthesize))
                .also(f)
                .action

            action += Action.Fork(methodsScope)
        }

        fun invokes(
            type: Entity.Type,
        ) {
            action += Action.Method.Invokes(type)
        }

        val Entity.Type.instantiations
            get() = EntityResolution.Instantiations(this)
        val Entity.Type.invocations
            get() = EntityResolution.Invocations(this)
    }

    class Parameters(
        parameters: Action<Iter<Element.Parameter>, Iter<Element.Parameter>> = Action.Parameter.ParameterScope
    ) : Core<Element.Parameter>(),
        CommonOperations<Element.Parameter, Parameters>,
        ParentOperations<Element.Method, Methods>
    {

        override var action: Action.Chain<IterParameters> = chainFrom(parameters)

        fun parameter(nth: Int) {
            action += Action.Parameter.FilterNth(nth)
        }

        override fun scope(
            @Suppress("UNUSED_PARAMETER") label: String,
            f: Parameters.() -> Unit
        ) {
            val forkTo = Parameters().also(f).action
            action += Action.Fork(forkTo)
        }

        override fun scope(
            label: String,
            op: ScopeEntityPredicate,
            entity: Entity.Type,
            f: Parameters.() -> Unit
        ) {
            val forkTo = Parameters().also(f).action
            action += Action.ForkOnEntityExistence(forkTo, entity, op == ifExistsNot)
        }

        override fun parentScope(label: String, f: Methods.() -> Unit) {
            val forkTo = Action.Parameter.IntoParents andThen Methods().also(f).action
            action += Action.Fork(forkTo)
        }

        override fun filter(regex: Regex, invert: Boolean) {
            action += Action.Parameter.Filter(regex, invert)
        }

        inline fun <reified T> annotatedBy() {
            annotatedBy(type<T>())
        }

        override fun annotatedBy(annotation: Type) {
            action += Action.HasAnnotation(annotation)
        }

        override fun readAnnotation(
            annotation: Type,
            field: String
        ): Action<IterParameters, IterValues> {
            val forkTo = Action.ReadAnnotation<Element.Parameter>(annotation, field)
                .let { Action.Fork(it) }

            action += forkTo

            return forkTo.forked
        }

        inline fun <reified T : Annotation> readAnnotation(
            field: KProperty1<T, *>
        ): Action<IterParameters, IterValues> = readAnnotation(type<T>(), field.name)


        fun readType(): Action<IterParameters, IterValues> {
            val forkTo = Action.Parameter.ReadType
                .let { Action.Fork(it) }

            action += forkTo

            return forkTo.forked
        }

        fun explodeType(synthesize: Boolean = false, f: Classes.() -> Unit) {
            val explodeType = Action.Parameter.ExplodeType(synthesize)
            val forkTo = Classes().also(f).action

            action += Action.Fork(explodeType andThen forkTo)
        }
    }

    class Fields(
        fields: Action<Iter<Element.Field>, Iter<Element.Field>> = Action.Field.FieldScope
    ) : Core<Element.Field>(),
        CommonOperations<Element.Field, Fields>,
        ParentOperations<Element.Class, Classes>
    {

        override var action: Action.Chain<IterFields> = chainFrom(fields)

        override fun scope(
            @Suppress("UNUSED_PARAMETER") label: String,
            f: Fields.() -> Unit
        ) {
            val forkTo = Fields().also(f).action
            action += Action.Fork(forkTo)
        }

        override fun scope(
            label: String,
            op: ScopeEntityPredicate,
            entity: Entity.Type,
            f: Fields.() -> Unit
        ) {
            val forkTo = Fields().also(f).action
            action += Action.ForkOnEntityExistence(forkTo, entity, op == ifExistsNot)
        }

        override fun parentScope(label: String, f: Classes.() -> Unit) {
            val forkTo = Action.Field.IntoParents andThen Classes().also(f).action
            action += Action.Fork(forkTo)
        }

        override fun filter(regex: Regex, invert: Boolean) {
            action += Action.Field.Filter(regex, invert)
        }

        inline fun <reified T> annotatedBy() = annotatedBy(type<T>())

        override fun annotatedBy(annotation: Type) {
            action += Action.HasAnnotation(annotation)
        }

        override fun readAnnotation(
            annotation: Type,
            field: String
        ): Action<IterFields, IterValues> {
            val forkTo = Action.ReadAnnotation<Element.Field>(annotation, field)
                .let { Action.Fork(it) }

            action += forkTo

            return forkTo.forked
        }

        inline fun <reified T : Annotation> readAnnotation(
            field: KProperty1<T, *>
        ): Action<IterFields, IterValues> = readAnnotation(type<T>(), field.name)
    }
}

sealed interface EntityResolver {
    val type: Entity.Type
    val id: String

    @NoArgConstructor
    class FromInvocationsOf(
        val key: String,
        override val type: Entity.Type
    ) : EntityResolver {
        override val id: String = "invocations"

        override fun resolve(
            ctx: Context,
            elements: Iter<Element.Method>
        ) {
            val matched: IdentityHashMap<MethodNode, Entity> = ctx.coercedMethodsOf(type)

            fun registerChildren(elem: Element.Method) {
                val parent = ctx.entityService[elem]!!
                ctx.methodsInvokedBy(elem.mn)
                    .filter { mn -> mn in matched }
                    .filter { mn -> elem.mn !== mn }
                    .map { ctx.entityService[matched[it]!!] as Element.Method }
                    .mapNotNull { ctx.entityService[it] }
                    .onEach { child -> parent.addChild(key, child) }
                    .onEach { child -> child.addChild("backtrack", parent) }
            }

            elements.forEach(::registerChildren)
        }
    }

    @NoArgConstructor
    class FromInstantiationsOf(
        val key: String,
        override val type: Entity.Type
    ) : EntityResolver {
        override val id: String = "instantiations"

        override fun resolve(
            ctx: Context,
            elements: Iter<Element.Method>
        ) {
            val types = ctx.entityService[type]
                    .map { (elem, _) -> elem as Element.Class } // FIXME: throw
                    .map(Element.Class::cn)
                    .map(ClassNode::type)

            fun registerChildren(elem: Element.Method) {
                val parent = ctx.entityService[elem]!!
                ctx.methodsInvokedBy(elem.mn)
                    .asSequence()
                    .flatMap { mn -> instantiations(mn, types) }
                    .distinct()
                    .map { type -> Element.Class(ctx.classByType[type]!!) }
                    .mapNotNull { ctx.entityService[it] }
                    .onEach { child -> parent.addChild(key, child) }
                    .forEach { child -> child.addChild("backtrack", parent) }
            }

            elements
                .forEach(::registerChildren)
        }

        private fun instantiations(mn: MethodNode, types: Iterable<Type>): List<Type> {
            return instantiations(mn).filter(types::contains)
        }
    }

    fun resolve(ctx: Context, elements: IterMethods)
}

fun Context.coercedMethodsOf(type: Entity.Type): IdentityHashMap<MethodNode, Entity> {
    fun toMethodNodes(elem: Element, e: Entity): List<Pair<MethodNode, Entity>> {
        return when (elem) {
            is Element.Class -> elem.cn.methods.map { mn -> mn to e }
            is Element.Method -> listOf(elem.mn to e)
            is Element.Parameter -> listOf(elem.mn to e)
            is Element.Value -> toMethodNodes(elem.reference, e)
            else -> error("unable to extract methods from $elem")
        }
    }

    return entityService[type]
        .flatMap { (elem, e) -> toMethodNodes(elem, e) } // FIXME: throw
        .toMap()
        .let(::IdentityHashMap)
}

sealed interface EntityResolution {
    class Instantiations(val type: Entity.Type) : EntityResolution
    class Invocations(val type: Entity.Type) : EntityResolution
}

@Suppress("EnumEntryName")
enum class ScopeEntityPredicate {
    ifExists, ifExistsNot
}