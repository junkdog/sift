package sift.core.dsl

import org.objectweb.asm.Type
import sift.core.*
import sift.core.api.*
import sift.core.api.Action.DebugLog.LogFormat
import sift.core.dsl.ScopeEntityPredicate.ifExistsNot
import sift.core.entity.Entity
import sift.core.entity.LabelFormatter
import sift.core.asm.type
import sift.core.element.*
import sift.core.terminal.TextTransformer
import java.util.*
import kotlin.reflect.KProperty1


@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class SiftTemplateDsl

@Suppress("UNCHECKED_CAST")
object Dsl {
    interface ParentOperations<T : Element, PARENT_SCOPE : Core<T>> {
        fun outerScope(label: String, f: PARENT_SCOPE.() -> Unit)
    }

    interface CommonOperations<T : Element, SCOPE : Core<T>> {
        fun filter(regex: Regex, invert: Boolean = false)

        fun filter(string: String, invert: Boolean = false) {
            filter(Regex.fromLiteral(string), invert)
        }

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

    /** Define a new template */
    fun template(f: Template.() -> Unit): Action<Unit, Unit> {
        return Template()
            .also(f)
            .action
    }

    /** Define a new template */
    fun classes(f: Classes.() -> Unit): Action<Unit, Unit> {
        return Classes()
            .also(f)
            .action
            .let(Action.Template.InstrumentClasses::andThen)
            .let { it andThen Action.Class.ToTemplateScope }
    }

    @SiftTemplateDsl
    abstract class Core<ELEMENT : Element> {

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
            action += Action.DebugLog(tag, format = LogFormat.Count)
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
        fun withValue(value: Any): Action<Iter<ELEMENT>, IterValues> {
            val forkTo = Action.WithValue<ELEMENT>(value)
                .let { Action.Fork(it) }

            action += forkTo

            return forkTo.forked
        }

        /**
         * Reads the short form name of the element
         */
        fun readName(shorten: Boolean = false): Action<Iter<ELEMENT>, IterValues> {
            val forkTo = Action.ReadName<ELEMENT>(shorten)
                .let { Action.Fork(it) }

            action +=  forkTo

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
    }

    @SiftTemplateDsl
    class Synthesize(
        var action: Action.Chain<Unit> = chainFrom(Action.Template.TemplateScope)
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
        fun label(
            pattern: String,
            vararg ops: TextTransformer
        ) = LabelFormatter.FromPattern(pattern, ops.toList())
    }

    @SiftTemplateDsl
    class Template(
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
            action += Action.ForkOnEntityExistence(forkTo, entity, op == ifExistsNot)
        }

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

        val Entity.Type.instantiations
            get() = EntityResolution.Instantiations(this)
        val Entity.Type.invocations
            get() = EntityResolution.Invocations(this)
        val Entity.Type.fieldAccess
            get() = EntityResolution.FieldAccess(this)
    }

    @SiftTemplateDsl
    class Signature(
        var action: Action.Chain<IterSignatures> = chainFrom(Action.Signature.SignatureScope)
    ) {
        fun readName(): Action<IterSignatures, IterValues> {
            val forkTo = Action.Signature.ReadSignature
                .let { Action.Fork(it) }

            action +=  forkTo

            return forkTo.forked
        }

        fun scope(label: String, f: Signature.() -> Unit) {
            action += Action.Fork(Signature().also(f).action)
        }

        fun filter(s: String, invert: Boolean = false) {
            action += Action.Signature.Filter(Regex.fromLiteral(s), invert)
        }

        fun filter(regex: Regex, invert: Boolean = false) {
            action += Action.Signature.Filter(regex, invert)
        }

        fun typeArguments(f: Signature.() -> Unit) {
            val inner = Signature().also(f).action
            action += Action.Fork(Action.Signature.InnerTypeArguments andThen inner)
        }

        fun typeArgument(index: Int, f: Signature.() -> Unit) {
            val filterNth = Action.Signature.FilterNth(index)
            val forkTo = Signature().also(f).action

            action += Action.Fork(filterNth andThen forkTo)
        }

        fun explodeType(synthesize: Boolean = false, f: Classes.() -> Unit) {
            val explodeType = Action.Signature.ExplodeType(synthesize)
            val forkTo = Classes().also(f).action

            action += Action.Fork(explodeType andThen forkTo)
        }

        /**
         * Iterates over all classes given a generic type signature, e.g.
         * `Map<_, List<T>>`. The signature parameter describes the generic
         * type to search for. It must contain a `T` token, which will be
         * replaced with each declaration during iteration.  The `_` symbol
         * can be used to match any class.
         *
         * Type constraints with names (e.g. String, Map) are only applied
         * to direct ancestors of `T`. In the signature `Pair<Foo, List<Map<Bar, T>>>`,
         * `Foo` and `Bar` are not directly related `T` and will therefore not be
         * evaluated.
         *
         * This function can greatly reduce the boilerplate associated with manually
         * unpacking type signatures. The following two templates are equivalent:
         *
         * ```kotlin
         * val a = classes {
         *     methods {
         *         returns {
         *             explodeTypeT("Map<_, List<Pair<T, _>>>", synthesize = true) {
         *                 entity(payload)
         *             }
         *         }
         *     }
         * }
         *
         * val b = classes {
         *     methods {
         *         returns {
         *             filter(Regex("^.+\\.Map\$"))
         *             typeArgument(1) {                     // List<Pair<Payload, Int>>
         *                 filter(Regex("^.+\\.List\$"))
         *                 typeArgument(0) {                 // Pair<Payload, Int>
         *                     filter(Regex("^.+\\.Pair\$"))
         *                     typeArgument(0) {             // Payload
         *                         explodeType(synthesize = true) {
         *                             entity(payload)
         *                         }
         *                     }
         *                 }
         *             }
         *         }
         *     }
         * }
         *
         * assert(a == b) { "expecting a and b to have the same underlying representation" }
         * ```
         */
        fun explodeTypeT(
            signature: String = "_<T>",
            synthesize: Boolean = false,
            f: Classes.() -> Unit
        ) {
            explodeTypeFromSignature(this, signature, synthesize, f)
        }

        /**
         * When `--debug` is passed to the CLI, prints [tag] and all elements
         * currently in scope.
         *
         * Note that for most use-cases, `--profile` yields better results
         * without having to modify the pipeline.
         **/
        fun log(tag: String) {
            action += Action.DebugLog(tag)
        }

        /**
         * When `--debug` is passed to the CLI, prints [tag] and the count
         * of elements currently in scope.
         *
         * Note that for most use-cases, `--profile` yields better results
         * without having to modify the template.
         **/
        fun logCount(tag: String) {
            action += Action.DebugLog(tag, format = LogFormat.Count)
        }
    }

    class Classes(
        override var action: Action.Chain<IterClasses> = chainFrom(Action.Class.ClassScope)
    ) : Core<ClassNode>(), CommonOperations<ClassNode, Classes>,
        ParentOperations<ClassNode, Classes>
    {
        // utility

        inline fun <reified T> annotatedBy() = annotatedBy(type<T>())

        override fun annotatedBy(annotation: Type) {
            action += Action.HasAnnotation(annotation)
        }

        override fun filter(regex: Regex, invert: Boolean) {
            action += Action.Class.Filter(regex, invert)
        }

        /** Filters the currently inspected class nodes by checking if they implement a particular type. */
        fun implements(type: AsmType) {
            action += Action.Class.FilterImplemented(type)
        }

        fun enums(f: Fields.() -> Unit) {
            action += Action.Fork(
                Action.Class.IntoEnumValues andThen Fields().also(f).action
            )
        }

        /**
         * Iterates the interfaces of current class nodes. Includes interfaces of super class.
         * Includes interfaces from all ancestors if [recursive] is `true`.
         */
        fun interfaces(recursive: Boolean = false, synthesize: Boolean = false, f: Classes.() -> Unit) {
            val scope = Classes().also(f).action
                .let { scope -> Action.Class.IntoInterfaces(recursive, synthesize) andThen scope }

            action += Action.Fork(scope)
        }

        /** filter elements by access modifiers */
        fun filter(
            vararg modifiers: Modifiers,
            invert: Boolean = false
        ) {
            filter(modifiers.toList(), invert)
        }


        /** filter elements by access modifiers */
        fun filter(
            modifiers: List<Modifiers>,
            invert: Boolean = false
        ) {
            action += Action.FilterModifiers(Modifiers.bitmaskOf(modifiers), invert)
        }

        override fun readAnnotation(
            annotation: Type,
            field: String
        ): Action<IterClasses, IterValues> {
            val forkTo = Action.ReadAnnotation<ClassNode>(annotation, field)
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

        /** iterates any outer classes */
        override fun outerScope(
            label: String,
            f: Classes.() -> Unit
        ) {
            val forkTo = Action.Class.IntoOuterClass andThen Classes().also(f).action
            action += Action.Fork(forkTo)
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

        fun superclass(f: Signature.() -> Unit) {
            val forkTo = Signature().also(f).action
                .let { signatureScope -> Action.Class.IntoSuperclassSignature andThen signatureScope }

            action += Action.Fork(forkTo)
        }
    }

    class Elements(
        elements: Action<Iter<Element>, Iter<Element>> = Action.Elements.ElementScope
    ) : Core<Element>() {
        override var action: Action.Chain<Iter<Element>> = chainFrom(elements)
    }

    class Methods(
        methods: Action<Iter<MethodNode>, Iter<MethodNode>> = Action.Method.MethodScope
    ) : Core<MethodNode>(),
        CommonOperations<MethodNode, Methods>,
        ParentOperations<ClassNode, Classes>
    {

        override var action: Action.Chain<IterMethods> = chainFrom(methods)

        operator fun Entity.Type.set(
            key: String,
            rhs: EntityResolution
        ) {
            val resolver = when (rhs) {
                is EntityResolution.Instantiations -> EntityAssignmentResolver.FromInstantiationsOf(key, rhs.type)
                is EntityResolution.Invocations    -> EntityAssignmentResolver.FromInvocationsOf(key, rhs.type)
                is EntityResolution.FieldAccess    -> EntityAssignmentResolver.FromFieldAccessOf(key, rhs.type)
            }

            action += Action.RegisterChildrenFromResolver(this, key, resolver)
        }

        operator fun EntityResolution.set(
            key: String,
            rhs: Entity.Type
        ) {
            val resolver = when (this) {
                is EntityResolution.Instantiations -> EntityAssignmentResolver.FromInstantiationsBy(key, type)
                is EntityResolution.Invocations    -> EntityAssignmentResolver.FromInvocationsBy(key, type)
                is EntityResolution.FieldAccess    -> EntityAssignmentResolver.FromFieldAccessBy(key, type)
            }

            action += Action.RegisterChildrenFromResolver(rhs, key, resolver)
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

        override fun outerScope(
            label: String,
            f: Classes.() -> Unit
        ) {
            val forkTo = Action.Method.IntoOuterScope andThen Classes().also(f).action
            action += Action.Fork(forkTo)
        }

        inline fun <reified T> annotatedBy() {
            annotatedBy(type<T>())
        }

        override fun filter(regex: Regex, invert: Boolean) {
            action += Action.Method.Filter(regex, invert)
        }

        fun filterName(regex: Regex, invert: Boolean = false) {
            action += Action.Method.FilterName(regex, invert)
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
            val forkTo = Action.ReadAnnotation<MethodNode>(annotation, field)
                .let { Action.Fork(it) }

            action += forkTo

            return forkTo.forked
        }

        inline fun <reified T : Annotation> readAnnotation(
            field: KProperty1<T, *>
        ): Action<IterMethods, IterValues> = readAnnotation(type<T>(), field.name)

        /** filter elements by access modifiers */
        fun filter(
            vararg modifiers: Modifiers,
            invert: Boolean = false
        ) {
            filter(modifiers.toList(), invert)
        }


        /** filter elements by access modifiers */
        fun filter(
            modifiers: List<Modifiers>,
            invert: Boolean = false
        ) {
            action += Action.FilterModifiers(Modifiers.bitmaskOf(modifiers), invert)
        }

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

        fun returns(f: Signature.() -> Unit) {
            val forkTo = Signature().also(f).action
                .let { signatureScope -> Action.Method.IntoReturnSignature andThen signatureScope }

            action += Action.Fork(forkTo)
        }

        val Entity.Type.instantiations
            get() = EntityResolution.Instantiations(this)
        val Entity.Type.invocations
            get() = EntityResolution.Invocations(this)
        val Entity.Type.fieldAccess
            get() = EntityResolution.FieldAccess(this)
    }

    class Parameters(
        parameters: Action<Iter<ParameterNode>, Iter<ParameterNode>> = Action.Parameter.ParameterScope
    ) : Core<ParameterNode>(),
        CommonOperations<ParameterNode, Parameters>,
        ParentOperations<MethodNode, Methods>
    {

        override var action: Action.Chain<IterParameters> = chainFrom(parameters)

        fun parameter(nth: Int) {
            action += Action.Parameter.FilterNth(nth)
        }

        fun signature(f: Signature.() -> Unit) {
            val forkTo = Signature().also(f).action
                .let { signatureScope -> Action.Parameter.IntoSignature andThen signatureScope }

            action += Action.Fork(forkTo)
        }

        /**
         * Filters fields to include only those with a type matching type.
         */
        fun filterType(type: AsmType) {
            action += Action.Parameter.FilterType(type)
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

        override fun outerScope(label: String, f: Methods.() -> Unit) {
            val forkTo = Action.Parameter.IntoOuterScope andThen Methods().also(f).action
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
            val forkTo = Action.ReadAnnotation<ParameterNode>(annotation, field)
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
        fields: Action<Iter<FieldNode>, Iter<FieldNode>> = Action.Field.FieldScope
    ) : Core<FieldNode>(),
        CommonOperations<FieldNode, Fields>,
        ParentOperations<ClassNode, Classes>
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

        override fun outerScope(label: String, f: Classes.() -> Unit) {
            val forkTo = Action.Field.IntoOuterScope andThen Classes().also(f).action
            action += Action.Fork(forkTo)
        }

        /** filter elements by access modifiers */
        fun filter(
            vararg modifiers: Modifiers,
            invert: Boolean = false
        ) {
            filter(modifiers.toList(), invert)
        }


        /** filter elements by access modifiers */
        fun filter(
            modifiers: List<Modifiers>,
            invert: Boolean = false
        ) {
            action += Action.FilterModifiers(Modifiers.bitmaskOf(modifiers), invert)
        }

        override fun filter(regex: Regex, invert: Boolean) {
            action += Action.Field.Filter(regex, invert)
        }

        /**
         * Filters fields to include only those with a type matching type.
         */
        fun filterType(type: AsmType) {
            action += Action.Field.FilterType(type)
        }

        fun explodeType(synthesize: Boolean = false, f: Classes.() -> Unit) {
            val explodeType = Action.Field.ExplodeType(synthesize)
            val forkTo = Classes().also(f).action

            action += Action.Fork(explodeType andThen forkTo)
        }

        inline fun <reified T> annotatedBy() = annotatedBy(type<T>())

        override fun annotatedBy(annotation: Type) {
            action += Action.HasAnnotation(annotation)
        }

        override fun readAnnotation(
            annotation: Type,
            field: String
        ): Action<IterFields, IterValues> {
            val forkTo = Action.ReadAnnotation<FieldNode>(annotation, field)
                .let { Action.Fork(it) }

            action += forkTo

            return forkTo.forked
        }

        inline fun <reified T : Annotation> readAnnotation(
            field: KProperty1<T, *>
        ): Action<IterFields, IterValues> = readAnnotation(type<T>(), field.name)

        fun signature(f: Signature.() -> Unit) {
            val forkTo = Signature().also(f).action
            action += Action.Fork(Action.Field.IntoSignature andThen forkTo)
        }
    }
}

sealed interface EntityResolution {
    class Instantiations(val type: Entity.Type) : EntityResolution
    class Invocations(val type: Entity.Type) : EntityResolution
    class FieldAccess(val type: Entity.Type) : EntityResolution
}

@Suppress("EnumEntryName")
enum class ScopeEntityPredicate {
    ifExists, ifExistsNot
}