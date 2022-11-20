package sift.core.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.onedaybeard.collectionsby.filterBy
import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import sift.core.entity.Entity
import sift.core.entity.LabelFormatter
import sift.core.Throw
import sift.core.UniqueElementPerEntityViolation
import sift.core.asm.*
import sift.core.jackson.NoArgConstructor

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Action.Instrumenter.InstrumenterScope::class, name = "instrumenter-scope"),
    JsonSubTypes.Type(value = Action.Instrumenter.InstrumentClasses::class, name = "classes"),
    JsonSubTypes.Type(value = Action.Instrumenter.ClassesOf::class, name = "classes-of"),
    JsonSubTypes.Type(value = Action.Instrumenter.MethodsOf::class, name = "methods-of"),

    JsonSubTypes.Type(value = Action.Class.ClassScope::class, name = "class-scope"),
    JsonSubTypes.Type(value = Action.Class.Filter::class, name = "filter-class"),
    JsonSubTypes.Type(value = Action.Class.FilterImplemented::class, name = "implements"),
    JsonSubTypes.Type(value = Action.Class.ToInstrumenterScope::class, name = "to-instrumenter-scope"),
    JsonSubTypes.Type(value = Action.Class.IntoMethods::class, name = "methods"),
    JsonSubTypes.Type(value = Action.Class.IntoOuterClass::class, name = "outer-class"),
    JsonSubTypes.Type(value = Action.Class.IntoFields::class, name = "fields"),
    JsonSubTypes.Type(value = Action.Class.ReadType::class, name = "read-type"),

    JsonSubTypes.Type(value = Action.Method.IntoParameters::class, name = "parameters"),
    JsonSubTypes.Type(value = Action.Method.IntoOuterScope::class, name = "method-parents"),
    JsonSubTypes.Type(value = Action.Method.MethodScope::class, name = "method-scope"),
    JsonSubTypes.Type(value = Action.Method.Filter::class, name = "filter-method"),
    JsonSubTypes.Type(value = Action.Method.Instantiations::class, name = "instantiations"),
    JsonSubTypes.Type(value = Action.Method.Invokes::class, name = "invokes"),
    JsonSubTypes.Type(value = Action.Method.InvocationsOf::class, name = "invocations-of"),

    JsonSubTypes.Type(value = Action.Field.FieldScope::class, name = "field-scope"),
    JsonSubTypes.Type(value = Action.Field.Filter::class, name = "filter-field"),
    JsonSubTypes.Type(value = Action.Field.ExplodeType::class, name = "field-explode-type"),
    JsonSubTypes.Type(value = Action.Field.IntoOuterScope::class, name = "field-parents"),

    JsonSubTypes.Type(value = Action.Parameter.ParameterScope::class, name = "parameter-scope"),
    JsonSubTypes.Type(value = Action.Parameter.ExplodeType::class, name = "explode-type"),
    JsonSubTypes.Type(value = Action.Parameter.ReadType::class, name = "read-type"),
    JsonSubTypes.Type(value = Action.Parameter.IntoOuterScope::class, name = "parameter-parents"),
    JsonSubTypes.Type(value = Action.Parameter.FilterNth::class, name = "parameter-nth"),
    JsonSubTypes.Type(value = Action.Parameter.Filter::class, name = "filter-parameter"),

    JsonSubTypes.Type(value = Action.DebugLog::class, name = "log"),
    JsonSubTypes.Type(value = Action.HasAnnotation::class, name = "has-annotation"),
    JsonSubTypes.Type(value = Action.EntityFilter::class, name = "entity-filter"),
    JsonSubTypes.Type(value = Action.ReadAnnotation::class, name = "read-annotation"),
    JsonSubTypes.Type(value = Action.WithValue::class, name = "with-value"),
    JsonSubTypes.Type(value = Action.ReadName::class, name = "read-name"),
    JsonSubTypes.Type(value = Action.Fork::class, name = "fork"),
    JsonSubTypes.Type(value = Action.ForkOnEntityExistence::class, name = "fork-conditional"),
    JsonSubTypes.Type(value = Action.RegisterEntity::class, name = "entity"),
    JsonSubTypes.Type(value = Action.RegisterSynthesizedEntity::class, name = "synthesize-entity"),
    JsonSubTypes.Type(value = Action.RegisterChildren::class, name = "children"),
    JsonSubTypes.Type(value = Action.RegisterChildrenFromResolver::class, name = "children-resolver"),
    JsonSubTypes.Type(value = Action.UpdateEntityProperty::class, name = "update-entity-property"),
    JsonSubTypes.Type(value = Action.Compose::class, name = "compose"),
    JsonSubTypes.Type(value = Action.Chain::class, name = "chain"),

    JsonSubTypes.Type(value = Action::class, name = "action"),
)
//@JsonSerialize(using = ActionSerializer::class)
@NoArgConstructor
sealed class Action<IN, OUT> {


    abstract fun id(): String
    abstract fun execute(ctx: Context, input: IN): OUT

    infix fun <T> andThen(action: Action<OUT, T>): Action<IN, T> = Compose(this, action)

    operator fun invoke(ctx: Context, input: IN): OUT {
        return ctx.measure(ctx, input, this)
    }

    object Instrumenter {
        object InstrumenterScope : Action<Unit, Unit>() {
            override fun id() = "instrumenter-scope"
            override fun execute(ctx: Context, input: Unit): Unit = input
        }

        object InstrumentClasses : Action<Unit, IterClasses>() {
            override fun id() = "classes"
            override fun execute(ctx: Context, input: Unit): IterClasses {
                return ctx.allClasses.map(Element::Class)
            }
        }

        data class ClassesOf(
            val entity: Entity.Type,
        ) : Action<Unit, IterClasses>() {
            override fun id() = "classes-of($entity)"
            override fun execute(ctx: Context, input: Unit): IterClasses {
                return ctx.entityService[entity].mapNotNull { (elem, _) ->
                    when (elem) {
                        is Element.Class -> elem
                        else ->  error("cannot iterate classes of ${elem::class.simpleName}: $elem")
                    }
                }
            }
        }

        data class MethodsOf(
            val entity: Entity.Type,
            ) : Action<Unit, IterMethods>() {
            override fun id() = "methods-of($entity)"
            override fun execute(ctx: Context, input: Unit): IterMethods {
                fun methodsOf(elem: Element.Class): Iterable<Element.Method> {
                    return elem.cn.methods.map { Element.Method(elem.cn, it) }
                        .onEach { output -> ctx.scopeTransition(elem, output) }
                }

                return ctx.entityService[entity].flatMap { (elem, _) ->
                    when (elem) {
                        is Element.Class  -> methodsOf(elem)
                        is Element.Method -> listOf(elem)
                        else              -> error("cannot iterate methods of ${elem::class.simpleName}: $elem")
                    }
                }
            }
        }

    }

    object Class {
        data class Filter(val regex: Regex, val invert: Boolean) : Action<IterClasses, IterClasses>() {
            override fun id() = "filter($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterClasses): IterClasses {
                val f = if (invert) input::filterNot else input::filter
                return f { regex in it.cn.qualifiedName }
            }
        }

        data class FilterImplemented(val type: Type) : Action<IterClasses, IterClasses>() {
            override fun id() = "implements(${type.simpleName})"
            override fun execute(ctx: Context, input: IterClasses): IterClasses {
                return input.filter { elem -> type in ctx.allInterfacesOf(elem.cn) }
            }
        }

        object ClassScope : Action<IterClasses, IterClasses>() {
            override fun id() = "class-scope"
            override fun execute(ctx: Context, input: IterClasses): IterClasses = input
        }

        object ToInstrumenterScope : Action<IterClasses, Unit>() {
            override fun id() = "instrumenter-scope"
            override fun execute(ctx: Context, input: IterClasses) {
                return Instrumenter.InstrumenterScope.invoke(ctx, Unit)
            }
        }

        object IntoMethods : Action<IterClasses, IterMethods>() {
            override fun id() = "methods"
            override fun execute(ctx: Context, input: IterClasses): IterMethods {
                fun methodsOf(input: Element.Class): Iterable<Element.Method> {
                    return input.cn.methods.map { Element.Method(input.cn, it) }
                        .onEach { output -> ctx.scopeTransition(input, output) }
                }

                return input.flatMap(::methodsOf)
            }
        }

        object IntoFields : Action<IterClasses, IterFields>() {
            override fun id() = "fields"
            override fun execute(ctx: Context, input: IterClasses): IterFields {
                fun methodsOf(input: Element.Class): IterFields {
                    return input.cn.fields.map { Element.Field(input.cn, it) }
                        .onEach { output -> ctx.scopeTransition(input, output) }
                }

                return input.flatMap(::methodsOf)
            }
        }

        object IntoOuterClass : Action<IterClasses, IterClasses>() {
            override fun id() = "outer-class"
            override fun execute(ctx: Context, input: IterClasses): IterClasses {
                fun outerClass(elem: Element.Class): Element.Class? {
                    val self = (elem.cn.innerClasses ?: listOf())
                        .firstOrNull { it.name == elem.cn.name }
                        ?: return null

                    return ctx.classByType[self.outerType]
                        ?.let(Element::Class)
                        ?.also { ctx.scopeTransition(elem, it) }
                }

                return input.mapNotNull(::outerClass)
            }
        }

        object ReadType : Action<IterClasses, IterValues>() {
            override fun id() = "read-type"
            override fun execute(ctx: Context, input: IterClasses): IterValues {
                return input
                    .map { Element.Value(it.cn.type, it) }
                    .onEach { ctx.scopeTransition(it.reference, it) }
            }
        }
    }

    object Method {
        object IntoParameters : Action<IterMethods, IterParameters>() {
            override fun id() = "parameters"
            override fun execute(ctx: Context, input: IterMethods): IterParameters {
                fun parametersOf(input: Element.Method): Iterable<Element.Parameter> {
                    return input.parameters()
                        .map { Element.Parameter(input.cn, input.mn, it) }
                        .onEach { output -> ctx.scopeTransition(input, output) }
                }

                return input.flatMap(::parametersOf)
            }
        }

        object IntoOuterScope : Action<IterMethods, IterClasses>() {
            override fun id() = "outer-class"
            override fun execute(ctx: Context, input: IterMethods): IterClasses {
                return input
                    .map { m -> m.into<Element.Class>().also { ctx.scopeTransition(m, it) } }
                    .toSet()
            }
        }

        object MethodScope : Action<IterMethods, IterMethods>() {
            override fun id() = "method-scope"
            override fun execute(ctx: Context, input: IterMethods): IterMethods = input
        }

        object DeclaredMethods : Action<IterMethods, IterMethods>() {
            override fun id() = "declared-scope"
            override fun execute(ctx: Context, input: IterMethods): IterMethods {
                return input
            }
        }

        data class Filter(val regex: Regex, val invert: Boolean) : Action<IterMethods, IterMethods>() {
            override fun id() = "filter($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterMethods): IterMethods {
                val f = if (invert) input::filterNot else input::filter
                return f { regex in it.mn.name || regex in it.cn.qualifiedName }
            }
        }
        
        data class Instantiations(
            val match: Entity.Type,
        ) : Action<IterMethods, IterClasses>() {
            override fun id() = "instantiations($match)"
            override fun execute(ctx: Context, input: IterMethods): IterClasses {
                val types = ctx.entityService[match]
                    .map { (elem, _) -> elem as Element.Class } // FIXME: throw
                    .map(Element.Class::cn)
                    .map(ClassNode::type)

                fun introspect(elem: Element.Method): List<Element.Class> {
                    return ctx.methodsInvokedBy(elem.mn)
                        .flatMap { mn -> instantiations(mn, types) }
                        .map { Element.Class(ctx.classByType[it]!!) }
                        .onEach { ctx.scopeTransition(elem, it) }
                }

                return input
                    .flatMap(::introspect)
            }

            private fun instantiations(mn: MethodNode, types: Iterable<Type>): List<Type> {
                return instantiations(mn).filter(types::contains)
            }
        }

        data class Invokes(
            val match: Entity.Type,
        )  : Action<IterMethods, IterMethods>() {
            override fun id() = "invokes($match)"
            override fun execute(ctx: Context, input: IterMethods): IterMethods {
                val matched = ctx.coercedMethodsOf(match)

                fun introspect(elem: Element.Method): List<Element.Method> {
                    return ctx.methodsInvokedBy(elem.mn)
                        .filter { mn -> mn in matched }
                        .filter { mn -> elem.mn !== mn }
                        .map { ctx.entityService[matched[it]!!] as Element.Method }
                        .onEach { ctx.scopeTransition(elem, it) }
                }

                return input
                    .filter { introspect(it).isNotEmpty() }
            }
        }

        data class InvocationsOf(
            val match: Entity.Type,
            val synthesize: Boolean
        ) : Action<IterMethods, IterMethods>() {
            override fun id() = "invocations-of($match${", synthesize".takeIf { synthesize } ?: ""})"
            override fun execute(ctx: Context, input: IterMethods): IterMethods {
                fun typeOf(elem: Element): Type {
                    return when (elem) {
                        is Element.Class -> elem.cn.type
                        is Element.Method -> elem.cn.type
                        is Element.Parameter -> elem.cn.type
                        is Element.Value -> typeOf(elem.reference)
                        else -> error("unable to extract methods from $elem")
                    }
                }

                val matched: Set<Type> = ctx.entityService[match]
                    .map { (elem, _) -> typeOf(elem) }
                    .toSet()

                fun invocations(mn: MethodNode): List<Invocation> {
                    val invocationsA = mn.asSequence()
                        .mapNotNull { it as? MethodInsnNode }
                        .map(::Invocation)
                        .toList()
                    val invocationsB = mn.asSequence()
                        .mapNotNull { it as? InvokeDynamicInsnNode }
                        .flatMap { ins -> ins.bsmArgs.mapNotNull { it as? Handle } }
                        .map(::Invocation)
                        .toList()

                    return invocationsA + invocationsB
                }

                fun methodElementOf(invocation: Invocation): Element.Method? {
                    val cn = ctx.classByType[invocation.type] ?: return null
                    val mn = when (synthesize) {
                        true -> ctx.synthesize(invocation.type, invocation.name, invocation.desc)
                        false -> cn.methods
                            .filterBy(MethodNode::name, invocation.name)
                            .filterBy(MethodNode::desc, invocation.desc)
                            .also { mns -> require(mns.size < 2) { error("$mns") } }
                            .firstOrNull()
                    } ?: return null

                    return Element.Method(cn, mn)
                }

                fun resolveInvocations(elem: Element.Method): List<Element.Method> {
                    return ctx.methodsInvokedBy(elem.mn)
                        .asSequence()
                        .flatMap(::invocations)
                        .distinct()
                        .filter { it.type in matched }
                        .mapNotNull(::methodElementOf)
                        .onEach { ctx.scopeTransition(elem, it) }
                        .toList()
                }

                return input
                    .flatMap(::resolveInvocations)
            }


            data class Invocation(
                val type: Type,
                val name: String,
                val desc: String
            ) {
                constructor(ins: MethodInsnNode) :
                    this(ins.ownerType, ins.name, ins.desc)
                constructor(handle: Handle) :
                    this(handle.ownerType, handle.name, handle.desc)
            }
        }
    }

    object Field {
        object FieldScope : Action<IterFields, IterFields>() {
            override fun id() = "field-scope"
            override fun execute(ctx: Context, input: IterFields): IterFields = input
        }

        object IntoOuterScope : Action<IterFields, IterClasses>() {
            override fun id() = "outer"
            override fun execute(ctx: Context, input: IterFields): IterClasses {
                return input
                    .map { f -> f.into<Element.Class>().also { ctx.scopeTransition(f, it) } }
                    .toSet()
            }
        }

        data class Filter(val regex: Regex, val invert: Boolean) : IsoAction<Element.Field>() {
            override fun id() = "filter($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterFields): IterFields {
                val f = if (invert) input::filterNot else input::filter
                return f { regex in it.fn.name }
            }
        }

        class ExplodeType(val synthesize: Boolean = false): Action<IterFields, IterClasses>() {
            override fun id() = "explode-type(${"synthesize".takeIf { synthesize } ?: ""})"
            override fun execute(ctx: Context, input: IterFields): IterClasses {
               fun explode(field: Element.Field): Element.Class? {
                   var exploded = ctx.classByType[field.fn.type]
                   if (exploded == null && synthesize)
                       exploded = ctx.synthesize(field.fn.type)

                   return Element.Class(exploded ?: return null)
                       .also { ctx.scopeTransition(field, it) }
               }

                return input.mapNotNull(::explode)
            }
        }
    }

    object Parameter {
        object ParameterScope : Action<IterParameters, IterParameters>() {
            override fun id() = "parameter-scope"
            override fun execute(ctx: Context, input: IterParameters): IterParameters = input
        }

        class ExplodeType(val synthesize: Boolean = false): Action<IterParameters, IterClasses>() {
            override fun id() = "explode-type(${"synthesize".takeIf { synthesize } ?: ""})"
            override fun execute(ctx: Context, input: IterParameters): IterClasses {
               fun explode(param: Element.Parameter): Element.Class? {

                   var exploded = ctx.classByType[param.pn.type]
                   if (exploded == null && synthesize)
                       exploded = ctx.synthesize(param.pn.type)

                   return Element.Class(exploded ?: return null)
                       .also { ctx.scopeTransition(param, it) }
               }

                return input.mapNotNull(::explode)
            }
        }

        object ReadType : Action<IterParameters, IterValues>() {
            override fun id() = "read-type"
            override fun execute(ctx: Context, input: IterParameters): IterValues {
                return input
                    .map { Element.Value(it.pn.type, it) }
                    .onEach { ctx.scopeTransition(it.reference, it) }
            }
        }

        object IntoOuterScope : Action<IterParameters, IterMethods>() {
            override fun id() = "outer"
            override fun execute(ctx: Context, input: IterParameters): IterMethods {
                return input
                    .map { p -> p.into<Element.Method>().also { ctx.scopeTransition(p, it) } }
                    .toSet()
            }
        }

        data class FilterNth(val nth: Int) : Action<IterParameters, IterParameters>() {
            override fun id() = "filter-nth($nth)"
            override fun execute(ctx: Context, input: IterParameters): IterParameters {
                return input.filter { it.mn.parameters?.indexOf(it.pn) == nth }
            }
        }

        data class Filter(val regex: Regex, val invert: Boolean) : IsoAction<Element.Parameter>() {
            override fun id() = "filter($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterParameters): IterParameters {
                val f = if (invert) input::filterNot else input::filter
                return f { regex in it.pn.name }
            }
        }
    }

    data class DebugLog<T : Element>(
        val tag: String,
        val format: LogFormat = LogFormat.Elements
    ) : IsoAction<T>() {
        override fun id() = "log($tag)"
        override fun execute(ctx: Context, input: Iter<T>): Iter<T> {
            if (!debugLog) return input
            val elements = input.toList().takeUnless(List<T>::isEmpty)
                ?: return input

            when (format) {
                LogFormat.Elements ->
                    "$tag:\n${elements.joinToString(prefix = "    ", separator = "\n    ") { it.simpleName }}"
                LogFormat.Count ->
                    "$tag: ${elements.size} ${elements.first()::class.simpleName!!.lowercase()}" +
                        if (elements.first() is Element.Class) "es" else "s"
            }.let(::println)

            return input
        }

        enum class LogFormat {
            Elements, Count
        }
    }

    data class HasAnnotation<T : Element>(val annotation: Type) : IsoAction<T>() {
        override fun id() = "annotated-by(${annotation.simpleName})"
        override fun execute(ctx: Context, input: Iter<T>): Iter<T> {
            return input
                .filter { annotation in it.annotations() }
        }
    }

    data class EntityFilter<T : Element>(val entity: Entity.Type) : IsoAction<T>() {
        override fun id() = "filter-entity($entity)"
        override fun execute(ctx: Context, input: Iter<T>): Iter<T> {
            return input.filter { it in ctx.entityService }
        }
    }

    class ReadName<T : Element>(val shortened: Boolean = false) : Action<Iter<T>, IterValues>() {
        override fun id() = "read-name"
        override fun execute(ctx: Context, input: Iter<T>): IterValues {
            fun nameOf(elem: T): String = when (elem) {
                is Element.Method    -> elem.mn.name
                is Element.Field     -> elem.fn.name
                is Element.Parameter -> elem.pn.name
                is Element.Class     -> {
                    if (shortened) {
                        (elem.cn.innerClasses ?: listOf())
                            .firstOrNull { it.name == elem.cn.name }
                            ?.innerName
                            ?: elem.cn.type.simpleName
                    } else {
                        elem.cn.type.simpleName
                    }
                }
                else                 -> error("$elem")
            }

            return input
                .map { Element.Value(nameOf(it), it) }
                .onEach { ctx.scopeTransition(it.reference, it) }
        }
    }

    data class ReadAnnotation<T: Element>(val annotation: Type, val field: String) : Action<Iter<T>, IterValues>() {
        override fun id() = "read-annotation(${annotation.simpleName}::$field)"
        override fun execute(ctx: Context, input: Iter<T>): IterValues {
            fun readAnnotation(input: T): Element.Value? {
                return input.annotations()
                    .find { an -> an.type == annotation }
                    ?.let { an -> readFieldAny(field)(an) }
                    ?.let { Element.Value(it, input) }
                    ?.also { ctx.scopeTransition(input, it) }
            }
            return input.mapNotNull(::readAnnotation)
        }

    }

    class WithValue<T : Element>(val value: Any) : Action<Iter<T>, IterValues>() {
        override fun id() = "with-value($value)"
        override fun execute(ctx: Context, input: Iter<T>): IterValues {
            return input
                .map { Element.Value(value, it) }
                .onEach { ctx.scopeTransition(it.reference, it) }
        }
    }

    data class Fork<T, FORK_T>(
        val forked: Action<T, FORK_T>
    ) : Action<T, T>() {
        override fun id() = "fork"
        override fun execute(ctx: Context, input: T): T {
            ctx.pushMeasurementScope()
            forked(ctx, input)
            ctx.popMeasurementScope()
            return input
        }
    }

    data class ForkOnEntityExistence<T, FORK_T>(
        val forked: Action<T, FORK_T>,
        val entity: Entity.Type,
        val invert: Boolean
    ) : Action<T, T>() {
        override fun id() = "fork-conditional($entity exists${"-not".takeIf { invert } ?: ""})"
        override fun execute(ctx: Context, input: T): T {
            if (entity in ctx.entityService != invert) {
                ctx.pushMeasurementScope()
                forked(ctx, input)
                ctx.popMeasurementScope()
            }

            return input
        }
    }

    data class RegisterEntity<T : Element>(
        val id: Entity.Type,
        val errorIfExists: Boolean,
        val labelFormatter: LabelFormatter
    ) : SimpleAction<T>() {
        override fun id() = "register-entity($id)"
        override fun invoke(ctx: Context, input: T): T {
            try {
                ctx.register(Entity(id, "N/A"), input, labelFormatter)
            } catch (e: UniqueElementPerEntityViolation) {
                if (errorIfExists)
                    throw e
            }

            return input
        }
    }

    data class RegisterSynthesizedEntity(
        val id: Entity.Type,
        val type: Type,
        val labelFormatter: LabelFormatter
    ) : Action<Unit, Unit>() {
        override fun id() = "register-entity-synthesized($id, ${type.simpleName})"
        override fun execute(ctx: Context, input: Unit) {
            ctx.synthesize(type)
                .let(Element::Class)
                .let { elem -> ctx.register(Entity(id, "N/A"), elem, labelFormatter) }

            return input
        }
    }

    data class RegisterChildren<T : Element>(
        val parentType: Entity.Type,
        val key: String,
        val childType: Entity.Type,
    ) : IsoAction<T>() {
        override fun id() = "register-children($parentType[$key], $childType)"
        override fun execute(ctx: Context, input: Iter<T>): Iter<T> {
            if (input.toList().isEmpty())
                return input
            if (childType !in ctx.entityService)
                Throw.entityNotRegistered(childType)
            if (parentType !in ctx.entityService)
                Throw.entityNotRegistered(parentType)

            fun relations(elem: Element, type: Entity.Type): Set<Entity> {
                val related = ctx.findRelatedEntities(elem, type)
                if (related.isEmpty() && elem !in ctx.entityService) {
                    Throw.entityNotFound(type, elem)
                }
                return related.filter { ctx.entityService[elem] != it }.toSet()
            }

            val a = ctx.entityService[parentType]
                .flatMap { (elem, parent) -> relations(elem, childType).map { parent to it } }
            val b = ctx.entityService[childType]
                .flatMap { (elem, child) -> relations(elem, parentType).map { it to child } }

            (a + b)
                .takeIf(List<Pair<Entity, Entity>>::isNotEmpty)
                ?.onEach { (parent, child) -> parent.addChild(key, child) }
                ?.onEach { (parent, child) -> child.addChild("backtrack", parent) }
                ?: Throw.unableToResolveParentRelation(parentType, childType)

            return input
        }
    }

    data class RegisterChildrenFromResolver(
        val parentType: Entity.Type,
        val key: String,
        val childResolver: EntityResolver,
    ) : IsoAction<Element.Method>() {
        override fun id() = "register-children($parentType, ${childResolver.type}.${childResolver.id})"
        override fun execute(ctx: Context, input: IterMethods): IterMethods {
            if (input.toList().isEmpty())
                return input

            if (parentType !in ctx.entityService)
                Throw.entityNotRegistered(parentType)

            childResolver.resolve(ctx, input)

            return input
        }
    }

    data class UpdateEntityProperty(
        val key: String,
        /** if null: resolves [Entity.Type] from currently referenced element */
        val entity: Entity.Type? = null
    ) : IsoAction<Element.Value>() {
        override fun id() = "update-property($key${", $entity".takeIf { entity != null } ?: ""})"
        override fun execute(ctx: Context, input: IterValues): IterValues {
            when (entity) {
                null -> {
                    input.map { it to ctx.entityService[it.reference] }
                        .forEach { (elem, e) -> e?.set(key, elem.data) }
                }
                else -> {
                    input.map { it to ctx.findRelatedEntities(it, entity) }
                        .forEach { (elem, e) -> e.forEach { it[key] = elem.data } }
                }
            }

            return input
        }
    }

    data class Compose<A, B, C>(
        val a: Action<A, B>,
        val b: Action<B, C>
    ) : Action<A, C>() {
        override fun id() = "composed"
        override fun execute(ctx: Context, input: A): C = b(ctx, a(ctx, input))
    }

    abstract class SimpleAction<T : Element> : Action<Iter<T>, Iter<T>>() {

        override fun execute(ctx: Context, input: Iter<T>): Iter<T> {
            return input.mapNotNull { t -> invoke(ctx, t) }
        }

        abstract fun invoke(ctx: Context, input: T): T?
    }

    data class Chain<T>(val actions: MutableList<Action<T, T>>) : Action<T, T>() {
        override fun id() = "chain"
        override fun execute(ctx: Context, input: T): T {
            return actions.fold(input) { elems, action -> action(ctx, elems) }
        }

        operator fun plusAssign(action: Action<T, T>) {
            actions += action
        }
    }
}

fun <T> chainFrom(action: Action<T, T>) = Action.Chain(mutableListOf(action))

private val InnerClassNode.outerType: Type
    get() = Type.getType("L${outerName};")

var debugLog = false