package sift.core.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import net.onedaybeard.collectionsby.filterBy
import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.Handle
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import sift.core.element.*
import sift.core.entity.Entity
import sift.core.entity.LabelFormatter
import sift.core.Throw
import sift.core.UnexpectedElementException
import sift.core.UniqueElementPerEntityViolation
import sift.core.asm.*
import sift.core.asm.signature.ArgType
import sift.core.dsl.PropertyStrategy
import sift.core.dsl.Type
import sift.core.element.ParameterNode
import sift.core.jackson.NoArgConstructor
import sift.core.terminal.StringEditor

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(Action.Template.TemplateScope::class, name = "template-scope"),
    JsonSubTypes.Type(Action.Template.InstrumentClasses::class, name = "classes"),
    JsonSubTypes.Type(Action.Template.ClassesOf::class, name = "classes-of"),
    JsonSubTypes.Type(Action.Template.FieldsOf::class, name = "fields-of"),
    JsonSubTypes.Type(Action.Template.MethodsOf::class, name = "methods-of"),
    JsonSubTypes.Type(Action.Template.ElementsOf::class, name = "elements-of"),

    JsonSubTypes.Type(Action.Signature.ExplodeType::class, name = "signature-explode-type"),
    JsonSubTypes.Type(Action.Signature.Filter::class, name = "filter-signature"),
    JsonSubTypes.Type(Action.Signature.FilterNth::class, name = "signature-filter-nth"),
    JsonSubTypes.Type(Action.Signature.InnerTypeArguments::class, name = "type-arguments"),
    JsonSubTypes.Type(Action.Signature.SignatureScope::class, name = "signature-scope"),

    JsonSubTypes.Type(Action.Elements.ElementScope::class, name = "elements-scope"),
    JsonSubTypes.Type(Action.Elements.Filter::class, name = "filter-elements"),

    JsonSubTypes.Type(Action.Class.ClassScope::class, name = "class-scope"),
    JsonSubTypes.Type(Action.Class.Filter::class, name = "filter-class"),
    JsonSubTypes.Type(Action.Class.FilterImplemented::class, name = "implements"),
    JsonSubTypes.Type(Action.Class.ToTemplateScope::class, name = "to-template-scope"),
    JsonSubTypes.Type(Action.Class.IntoEnumValues::class, name = "into-enum-values"),
    JsonSubTypes.Type(Action.Class.IntoMethods::class, name = "methods"),
    JsonSubTypes.Type(Action.Class.IntoOuterClass::class, name = "outer-class"),
    JsonSubTypes.Type(Action.Class.IntoFields::class, name = "fields"),
    JsonSubTypes.Type(Action.Class.IntoInterfaces::class, name = "into-interfaces"),
    JsonSubTypes.Type(Action.Class.IntoSuperclassSignature::class, name = "superclass"),
    JsonSubTypes.Type(Action.Class.ReadType::class, name = "read-type"),

    JsonSubTypes.Type(Action.Method.IntoParameters::class, name = "parameters"),
    JsonSubTypes.Type(Action.Method.IntoOuterScope::class, name = "method-parents"),
    JsonSubTypes.Type(Action.Method.IntoReturnSignature::class, name = "returns"),
    JsonSubTypes.Type(Action.Method.MethodScope::class, name = "method-scope"),
    JsonSubTypes.Type(Action.Method.FieldAccess::class, name = "field-access"),
    JsonSubTypes.Type(Action.Method.Filter::class, name = "filter-method"),
    JsonSubTypes.Type(Action.Method.FilterName::class, name = "filter-method-name"),
    JsonSubTypes.Type(Action.Method.Instantiations::class, name = "instantiations"),
    JsonSubTypes.Type(Action.Method.Invokes::class, name = "invokes"),
    JsonSubTypes.Type(Action.Method.InvocationsOf::class, name = "invocations-of"),

    JsonSubTypes.Type(Action.Field.FieldScope::class, name = "field-scope"),
    JsonSubTypes.Type(Action.Field.Filter::class, name = "filter-field"),
    JsonSubTypes.Type(Action.Field.FilterType::class, name = "filter-type-field"),
    JsonSubTypes.Type(Action.Field.ExplodeType::class, name = "field-explode-type"),
    JsonSubTypes.Type(Action.Field.IntoOuterScope::class, name = "field-parents"),
    JsonSubTypes.Type(Action.Field.IntoSignature::class, name = "field-signature"),

    JsonSubTypes.Type(Action.Parameter.ParameterScope::class, name = "parameter-scope"),
    JsonSubTypes.Type(Action.Parameter.ExplodeType::class, name = "explode-type"),
    JsonSubTypes.Type(Action.Parameter.ReadType::class, name = "read-type"),
    JsonSubTypes.Type(Action.Parameter.IntoOuterScope::class, name = "parameter-parents"),
    JsonSubTypes.Type(Action.Parameter.IntoSignature::class, name = "parameter-signature"),
    JsonSubTypes.Type(Action.Parameter.FilterNth::class, name = "parameter-nth"),
    JsonSubTypes.Type(Action.Parameter.Filter::class, name = "filter-parameter"),
    JsonSubTypes.Type(Action.Parameter.FilterType::class, name = "filter-type-parameter"),

    JsonSubTypes.Type(Action.DebugLog::class, name = "log"),
    JsonSubTypes.Type(Action.HasAnnotation::class, name = "has-annotation"),
    JsonSubTypes.Type(Action.EntityFilter::class, name = "entity-filter"),
    JsonSubTypes.Type(Action.FilterModifiers::class, name = "filter-modifiers"),
    JsonSubTypes.Type(Action.ReadAnnotation::class, name = "read-annotation"),
    JsonSubTypes.Type(Action.WithValue::class, name = "with-value"),
    JsonSubTypes.Type(Action.EditText::class, name = "edit-text"),
    JsonSubTypes.Type(Action.ReadName::class, name = "read-name"),
    JsonSubTypes.Type(Action.Fork::class, name = "fork"),
    JsonSubTypes.Type(Action.ForkOnEntityExistence::class, name = "fork-conditional"),
    JsonSubTypes.Type(Action.RegisterEntity::class, name = "entity"),
    JsonSubTypes.Type(Action.RegisterSynthesizedEntity::class, name = "synthesize-entity"),
    JsonSubTypes.Type(Action.RegisterChildren::class, name = "children"),
    JsonSubTypes.Type(Action.RegisterChildrenFromResolver::class, name = "children-resolver"),
    JsonSubTypes.Type(Action.UpdateEntityProperty::class, name = "update-entity-property"),
    JsonSubTypes.Type(Action.Compose::class, name = "compose"),
    JsonSubTypes.Type(Action.Chain::class, name = "chain"),

    JsonSubTypes.Type(Action::class, name = "action"),
)
@NoArgConstructor
sealed class Action<IN, OUT> {

    abstract fun id(): String
    internal abstract fun execute(ctx: Context, input: IN): OUT

    infix fun <T> andThen(action: Action<OUT, T>): Action<IN, T> = Compose(this, action)

    internal operator fun invoke(ctx: Context, input: IN): OUT {
        return ctx.measure(ctx, input, this)
    }

    internal object Template {
        internal object TemplateScope : Action<Unit, Unit>() {
            override fun id() = "template-scope"
            override fun execute(ctx: Context, input: Unit): Unit = input
        }

        internal object InstrumentClasses : Action<Unit, IterClasses>() {
            override fun id() = "classes"
            override fun execute(ctx: Context, input: Unit): IterClasses {
                return ctx.allClasses
            }
        }

        internal data class ClassesOf(
            val entity: Entity.Type,
        ) : Action<Unit, IterClasses>() {
            override fun id() = "classes-of($entity)"
            override fun execute(ctx: Context, input: Unit): IterClasses {
                return ctx.entityService[entity].mapNotNull { (elem, _) ->
                    when (elem) {
                        is ClassNode -> elem
                        else ->  error("cannot iterate classes of ${elem::class.simpleName}: $elem")
                    }
                }
            }
        }

        internal data class ElementsOf(
            val entity: Entity.Type,
        ) : Action<Unit, Iter<Element>>() {
            override fun id() = "elements-of($entity)"
            override fun execute(ctx: Context, input: Unit): Iter<Element> {
                return ctx.entityService[entity].map { (elem, _) -> elem }
            }
        }

        internal data class FieldsOf(
            val entity: Entity.Type,
        ) : Action<Unit, IterFields>() {
            override fun id() = "fields-of($entity)"
            override fun execute(ctx: Context, input: Unit): IterFields {
                return ctx.entityService[entity].map { (elem, _) ->
                    when (elem) {
                        is FieldNode -> elem
                        else ->  error("cannot iterate fields of ${elem::class.simpleName}: $elem")
                    }
                }
            }
        }

        internal data class MethodsOf(
            val entity: Entity.Type,
        ) : Action<Unit, IterMethods>() {
            override fun id() = "methods-of($entity)"
            override fun execute(ctx: Context, input: Unit): IterMethods {
                fun methodsOf(elem: ClassNode): Iterable<MethodNode> {
                    return elem.methods
                        .onEach { output -> ctx.scopeTransition(elem, output) }
                }

                return ctx.entityService[entity].flatMap { (elem, _) ->
                    when (elem) {
                        is ClassNode  -> methodsOf(elem)
                        is MethodNode -> listOf(elem)
                        else          -> error("cannot iterate methods of ${elem::class.simpleName}: $elem")
                    }
                }
            }
        }

    }

    internal object Elements {
        internal object ElementScope : Action<Iter<Element>, Iter<Element>>() {
            override fun id() = "element-scope"
            override fun execute(ctx: Context, input: Iter<Element>): Iter<Element> = input
        }

        internal data class Filter(
            val regex: Regex, val invert: Boolean,
        ) : Action<Iter<Element>, Iter<Element>>() {
            override fun id() = "filter-elements"

            @Suppress("UNCHECKED_CAST")
            override fun execute(ctx: Context, input: Iter<Element>): Iter<Element> {
                val elems = input.toList()
                    .takeIf(List<Element>::isNotEmpty)
                    ?: return input

                return when (val elem = elems.first()) {
                    is ClassNode     -> Class.Filter(regex, invert).execute(ctx, elems as IterClasses)
                    is FieldNode     -> Field.Filter(regex, invert).execute(ctx, elems as IterFields)
                    is MethodNode    -> Method.Filter(regex, invert).execute(ctx, elems as IterMethods)
                    is ParameterNode -> Parameter.Filter(regex, invert).execute(ctx, elems as IterParameters)
                    is SignatureNode -> Signature.Filter(regex, invert).execute(ctx, elems as IterSignatures)
                    else -> error("cannot filter elements of ${elem::class.simpleName}")
                }
            }
        }
    }

    internal object Signature {
        internal data class ExplodeType(val synthesize: Boolean) : Action<IterSignatures, IterClasses>() {
            override fun id() = "explode-raw-type"
            override fun execute(ctx: Context, input: IterSignatures): IterClasses {
                fun classOf(elem: SignatureNode): ClassNode? {
                    val type = (elem.argType as? ArgType.Plain)?.type ?: return null
                    return (ctx.classByType[type] ?: if (synthesize) ctx.synthesize(type) else null)
                        ?.also { output -> ctx.scopeTransition(elem, output) }
                }

                return input.mapNotNull(::classOf)
            }
        }

        internal object SignatureScope : Action<IterSignatures, IterSignatures>() {
            override fun id() = "signature-scope"
            override fun execute(ctx: Context, input: IterSignatures) = input
        }

        internal object InnerTypeArguments : Action<IterSignatures, IterSignatures>() {
            override fun id() = "inner-type-arguments"
            override fun execute(ctx: Context, input: IterSignatures): IterSignatures {
                fun argumentsOf(elem: SignatureNode): Iterable<SignatureNode> {
                    return elem.inner
                        .onEach { output -> ctx.scopeTransition(elem, output) }
                }

                return input.flatMap(::argumentsOf)
            }
        }

        internal data class Filter(val regex: Regex, val invert: Boolean) : Action<IterSignatures, IterSignatures>() {
            override fun id() = "filter-signature($regex${" invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterSignatures): IterSignatures {
                fun classNameOf(elem: SignatureNode): String? =
                    (elem.argType as? ArgType.Plain)?.type?.className

                return input
                    .filter { classNameOf(it)?.let { desc -> (regex in desc) xor invert } == true }
            }
        }

        internal object ReadSignature : Action<IterSignatures, IterValues>() {
            override fun id() = "read-name"
            override fun execute(ctx: Context, input: IterSignatures): IterValues {
                return input
                    .map { ValueNode.from(it.simpleName, it) }
                    .onEach { ctx.scopeTransition(it.reference, it) }
            }
        }

        internal data class FilterNth(
            val nth: Int,
        ) : Action<IterSignatures, IterSignatures>() {
            override fun id() = "filter-nth($nth)"
            override fun execute(ctx: Context, input: IterSignatures): IterSignatures {
                fun resolve(elem: SignatureNode): SignatureNode? {
                    return elem.inner.getOrNull(nth)
                        ?.also { output -> ctx.scopeTransition(elem, output) }
                }

                return input.mapNotNull(::resolve)
            }
        }
    }

    internal object Class {
        internal data class Filter(val regex: Regex, val invert: Boolean) : Action<IterClasses, IterClasses>() {
            override fun id() = "filter($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterClasses): IterClasses {
                return input.filter { cn -> (regex in cn.qualifiedName) xor invert }
            }
        }

        internal data class IntoInterfaces(
            val recursive: Boolean,
            val synthesize: Boolean
        ) : Action<IterClasses, IterClasses>() {

            override fun id(): String {
                val params = listOfNotNull(
                    "recursive".takeIf { recursive },
                    "synthesize".takeIf { synthesize },
                )
                return "into-interfaces(${params.joinToString(separator = " ")})"
            }

            override fun execute(ctx: Context, input: IterClasses): IterClasses {
                val resolveClassNode: (Type) -> ClassNode? = when {
                    synthesize -> { type -> ctx.synthesize(type.asmType) }
                    else       -> { type -> ctx.classByType[type.asmType] }
                }

                fun allInterfacesOf(elem: ClassNode): Iterable<ClassNode> {
                    // fixme: not correctly resolving generic interface signatures; ref Context
                    val parentInterfaces = ctx.parents[elem]!!
                        .map { tcn -> tcn.cn!! }
                        .flatMap(ClassNode::interfaces)
                        .map(Type::from)

                    return (parentInterfaces + ctx.allInterfacesOf(elem, false))
                        .toSet()
                        .mapNotNull(resolveClassNode)
                        .onEach { output -> ctx.scopeTransition(elem, output) }
                }

                fun interfacesOf(elem: ClassNode): Iterable<ClassNode> {
                    // fixme: not correctly resolving generic interface signatures; ref Context
                    val parentInterfaces = elem.superType
                        ?.let(ctx.classByType::get)
                        ?.interfaces
                        ?.map(Type::from)
                        ?: listOf()

                    return (parentInterfaces + elem.interfaces.map(Type::from))
                        .toSet()
                        .mapNotNull(resolveClassNode)
                        .onEach { output -> ctx.scopeTransition(elem, output) }
                }

                return when {
                    recursive -> input.flatMap(::allInterfacesOf)
                    else      -> input.flatMap(::interfacesOf)
                }
            }
        }

        internal data class FilterImplemented(val type: Type) : Action<IterClasses, IterClasses>() {
            override fun id() = "implements(${type.simpleName})"
            override fun execute(ctx: Context, input: IterClasses): IterClasses {
                return input.filter { elem -> type matches ctx.allInterfacesOf(elem) }
            }
        }

        internal object ClassScope : Action<IterClasses, IterClasses>() {
            override fun id() = "class-scope"
            override fun execute(ctx: Context, input: IterClasses): IterClasses = input
        }

        internal object IntoSuperclassSignature : Action<IterClasses, IterSignatures>() {
            override fun id() = "into-superclass-signature"
            override fun execute(ctx: Context, input: IterClasses): IterSignatures {
                fun signatureOf(elem: ClassNode): SignatureNode? {
                    return elem.signature?.extends
                        ?.let(SignatureNode::from)
                        ?.also { output -> ctx.scopeTransition(elem, output) }
                }

                return input.mapNotNull(::signatureOf)
            }
        }

        internal object IntoEnumValues : Action<IterClasses, IterFields>() {
            override fun id() = "into-enum-values"
            override fun execute(ctx: Context, input: IterClasses): IterFields {
                fun enumValuesOf(elem: ClassNode): List<FieldNode> {
                    return elem.fields
                        .filter { it.isStatic && it.isFinal && it.isEnum }
                        .onEach { output -> ctx.scopeTransition(elem, output) }
                }

                return input
                    .filter(ClassNode::isEnum)
                    .flatMap(::enumValuesOf)
            }
        }

        internal object ToTemplateScope : Action<IterClasses, Unit>() {
            override fun id() = "template-scope"
            override fun execute(ctx: Context, input: IterClasses) {
                return Template.TemplateScope.invoke(ctx, Unit)
            }
        }

        internal data class IntoMethods(
            val inherited: Boolean,
        ) : Action<IterClasses, IterMethods>() {
            override fun id() = "methods"
            override fun execute(ctx: Context, input: IterClasses): IterMethods {
                fun inheritedMethodsOf(input: ClassNode): IterMethods {
                    return ctx.parents[input]!!
                        .mapNotNull { (_, cn) ->  cn?.methods }
                        .flatten()
                }

                fun methodsOf(input: ClassNode): IterMethods {
                    val inheritedMethods = if (inherited) inheritedMethodsOf(input) else emptyList()
                    return (input.methods + inheritedMethods)
                        .onEach { output -> ctx.scopeTransition(input, output) }
                }

                return input.flatMap(::methodsOf)
                    .let { if (inherited) it.toSet() else it }
            }
        }

        internal data class IntoFields(
            val inherited: Boolean,
        ) : Action<IterClasses, IterFields>() {
            override fun id() = "fields(${"inherited".takeIf { inherited } ?: ""})"
            override fun execute(ctx: Context, input: IterClasses): IterFields {
                fun inheritedFieldsOf(input: ClassNode): IterFields {
                    return ctx.parents[input]!!
                        .mapNotNull { (_, cn) ->  cn?.fields }
                        .flatten()
                }

                fun fieldsOf(input: ClassNode): IterFields {
                    val inheritedFields = if (inherited) inheritedFieldsOf(input) else emptyList()

                    return (input.fields + inheritedFields)
                            .onEach { output -> ctx.scopeTransition(input, output) }
                }

                return input.flatMap(::fieldsOf)
                    .let { if (inherited) it.toSet() else it }
            }
        }

        internal object IntoOuterClass : Action<IterClasses, IterClasses>() {
            override fun id() = "outer-class"
            override fun execute(ctx: Context, input: IterClasses): IterClasses {
                fun outerClass(elem: ClassNode): ClassNode? {
                    val outer = elem.outerType ?: return null

                    return ctx.classByType[outer]
                        ?.also { ctx.scopeTransition(elem, it) }
                }

                return input.mapNotNull(::outerClass)
            }
        }

        internal object ReadType : Action<IterClasses, IterValues>() {
            override fun id() = "read-type"
            override fun execute(ctx: Context, input: IterClasses): IterValues {
                return input
                    .map { cn -> ValueNode.from(cn.type, cn) }
                    .onEach { ctx.scopeTransition(it.reference, it) }
            }
        }
    }

    internal object Method {
        internal object IntoReturnSignature : Action<IterMethods, IterSignatures>() {
            override fun id() = "returns"
            override fun execute(ctx: Context, input: IterMethods): IterSignatures {
                fun signatureOf(elem: MethodNode): SignatureNode? {
                    return elem.returns()
                        ?.also { output -> ctx.scopeTransition(elem, output) }
                }

                return input.mapNotNull(::signatureOf)
            }
        }

        internal object IntoParameters : Action<IterMethods, IterParameters>() {
            override fun id() = "parameters"
            override fun execute(ctx: Context, input: IterMethods): IterParameters {
                fun parametersOf(input: MethodNode): IterParameters {
                    return input.parameters
                        .onEach { output -> ctx.scopeTransition(input, output) }
                }

                return input.flatMap(::parametersOf)
            }
        }

        internal object IntoOuterScope : Action<IterMethods, IterClasses>() {
            override fun id() = "outer-class"
            override fun execute(ctx: Context, input: IterMethods): IterClasses {
                return input
                    .map { m -> m.owner.also { ctx.scopeTransition(m, it) } }
                    .toSet()
            }
        }

        internal object MethodScope : Action<IterMethods, IterMethods>() {
            override fun id() = "method-scope"
            override fun execute(ctx: Context, input: IterMethods): IterMethods = input
        }

        internal data class Filter(val regex: Regex, val invert: Boolean) : Action<IterMethods, IterMethods>() {
            override fun id() = "filter($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterMethods): IterMethods {
                return input.filter { (regex in it.name || regex in it.owner.qualifiedName) xor invert }
            }
        }

        internal data class FilterName(val regex: Regex, val invert: Boolean) : Action<IterMethods, IterMethods>() {
            override fun id() = "filter-name($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterMethods): IterMethods {
                return input.filter { (regex in it.name) xor invert }
            }
        }

        object FieldAccess : Action<IterMethods, IterFields>() {
            override fun id() = "field-access"
            override fun execute(ctx: Context, input: IterMethods): IterFields {
                fun resolveFieldNode(ins: FieldInsnNode): FieldNode? = ctx
                    .classByType[ins.ownerType]
                    ?.fields
                    ?.findBy(FieldNode::name, ins.name)

                fun fieldsOf(elem: MethodNode): List<FieldNode> = ctx
                    .methodsInvokedBy(elem)
                    .flatMap(MethodNode::instructions)
                    .mapNotNull { ins -> ins as? FieldInsnNode }
                    .mapNotNull(::resolveFieldNode)
                    .onEach { output -> ctx.scopeTransition(elem, output) }

                return input.flatMap(::fieldsOf)
            }
        }

        internal data class Instantiations(
            val match: Entity.Type,
        ) : Action<IterMethods, IterClasses>() {
            override fun id() = "instantiations($match)"
            override fun execute(ctx: Context, input: IterMethods): IterClasses {

                val types = ctx.entities<ClassNode>(match)
                    .map { (elem, _) -> elem }
                    .map(ClassNode::rawType)

                fun introspect(elem: MethodNode): List<ClassNode> {
                    return ctx.methodsInvokedBy(elem)
                        .flatMap { mn -> instantiations(mn, types) }
                        .map { ctx.classByType[it]!! }
                        .onEach { ctx.scopeTransition(elem, it) }
                }

                return input
                    .flatMap(::introspect)
            }

            private fun instantiations(mn: MethodNode, types: Iterable<AsmType>): List<AsmType> {
                return instantiations(mn).filter(types::contains)
            }
        }

        internal data class Invokes(
            val match: Entity.Type,
        )  : Action<IterMethods, IterMethods>() {
            override fun id() = "invokes($match)"
            override fun execute(ctx: Context, input: IterMethods): IterMethods {
                val matched = ctx.coercedMethodsOf(match)

                fun introspect(elem: MethodNode): List<MethodNode> {
                    return ctx.methodsInvokedBy(elem)
                        .filter { mn -> mn in matched }
                        .filter { mn -> elem != mn }
                        .onEach { ctx.scopeTransition(elem, it) }
                }

                return input
                    .filter { introspect(it).isNotEmpty() }
            }
        }

        @OptIn(FlowPreview::class)
        internal data class InvocationsOf(
            val match: Entity.Type,
            val synthesize: Boolean
        ) : Action<IterMethods, IterMethods>() {
            override fun id() = "invocations-of($match${", synthesize".takeIf { synthesize } ?: ""})"
            override fun execute(ctx: Context, input: IterMethods): IterMethods {
                fun typeOf(arg: ArgType): AsmType {
                    return when (arg) {
                        is ArgType.Array -> typeOf(arg.wrapped!!)
                        is ArgType.Plain -> arg.type
                        is ArgType.Var -> TODO("typeOf not implemented for formal type variables")
                    }
                }

                fun typeOf(elem: Element): AsmType {
                    return when (elem) {
                        is ClassNode     -> elem.rawType
                        is MethodNode    -> elem.owner.rawType
                        is ParameterNode -> elem.owner.owner.rawType
                        is ValueNode     -> typeOf(elem.reference)
                        is SignatureNode -> typeOf(elem.argType)
                        is FieldNode     -> elem.rawType
                        else             -> error("unable to extract type of $elem")
                    }
                }

                val matched: Set<AsmType> = ctx.entityService[match]
                    .map { (elem, _) -> typeOf(elem) }
                    .toSet()

                fun invocations(mn: MethodNode): Flow<Invocation> {
                    val invocationsA = mn.instructions()
                        .mapNotNull { it as? MethodInsnNode }
                        .map(::Invocation)
                        .toList()
                    val invocationsB = mn.instructions()
                        .mapNotNull { it as? InvokeDynamicInsnNode }
                        .flatMap { ins -> ins.bsmArgs.mapNotNull { it as? Handle } }
                        .map(::Invocation)
                        .toList()

                    return (invocationsA + invocationsB).asFlow()
                }

                fun methodElementOf(invocation: Invocation): MethodNode? {
                    val cn = ctx.classByType[invocation.type] ?: return null
                    return when (synthesize) {
                        true -> ctx.synthesize(invocation.type, invocation.name, invocation.desc)
                        false -> cn.methods
                            .filterBy(MethodNode::name, invocation.name)
                            .filterBy(MethodNode::desc, invocation.desc)
                            .also { mns -> require(mns.size < 2) { error("$mns") } }
                            .firstOrNull()
                    }
                }

                fun resolveInvocations(elem: MethodNode): Flow<Pair<MethodNode, MethodNode>> {
                    return ctx.methodsInvokedBy(elem)
                        .asFlow()
                        .flatMapConcat(::invocations)
                        .filter { it.type in matched }
                        .mapNotNull(::methodElementOf)
                        .map { elem to it }
                }

                return runBlocking(Dispatchers.Default) {
                    input.asFlow()
                        .flatMapConcat { mn -> resolveInvocations(mn) }
                            .toList()
                            .distinctBy { (_, mn) -> mn }
                            .onEach { (old, mn) -> ctx.scopeTransition(old, mn) }
                            .map { (_, mn) -> mn }

                }
            }

            internal data class Invocation(
                val type: AsmType,
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

    internal object Field {
        internal object FieldScope : Action<IterFields, IterFields>() {
            override fun id() = "field-scope"
            override fun execute(ctx: Context, input: IterFields): IterFields = input
        }

        internal object IntoSignature : Action<IterFields, IterSignatures>() {
            override fun id() = "field-into-signature"
            override fun execute(ctx: Context, input: IterFields): IterSignatures {
                fun signatureOf(elem: FieldNode): SignatureNode? {
                    return elem.returns
                        ?.also { output -> ctx.scopeTransition(elem, output) }
                }

                return input
                    .mapNotNull(::signatureOf)
            }
        }

        internal object IntoOuterScope : Action<IterFields, IterClasses>() {
            override fun id() = "outer"
            override fun execute(ctx: Context, input: IterFields): IterClasses {
                return input
                    .map { f -> f.owner.also { ctx.scopeTransition(f, it) } }
                    .toSet()
            }
        }

        internal data class Filter(val regex: Regex, val invert: Boolean) : IsoAction<FieldNode>() {
            override fun id() = "filter($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterFields): IterFields {
                val f = if (invert) input::filterNot else input::filter
                return f { regex in it.name }
            }
        }

        internal data class FilterType(val type: AsmType) : IsoAction<FieldNode>() {
            override fun id() = "filter-type(${type.simpleName})"
            override fun execute(ctx: Context, input: IterFields): IterFields {
                return input.filterBy(FieldNode::rawType, type)
            }
        }

        class ExplodeType(val synthesize: Boolean = false): Action<IterFields, IterClasses>() {
            override fun id() = "explode-type(${"synthesize".takeIf { synthesize } ?: ""})"
            override fun execute(ctx: Context, input: IterFields): IterClasses {
               fun explode(field: FieldNode): ClassNode? {
                   var exploded = ctx.classByType[field.rawType]
                   if (exploded == null && synthesize)
                       exploded = ctx.synthesize(field.rawType)

                   return exploded
                       ?.also { ctx.scopeTransition(field, it) }
               }

                return input.mapNotNull(::explode)
            }
        }
    }

    internal object Parameter {
        internal object ParameterScope : Action<IterParameters, IterParameters>() {
            override fun id() = "parameter-scope"
            override fun execute(ctx: Context, input: IterParameters): IterParameters = input
        }

        internal object IntoSignature : Action<IterParameters, IterSignatures>() {
            override fun id() = "parameter-into-signature"
            override fun execute(ctx: Context, input: IterParameters): IterSignatures {
                fun signatureOf(elem: ParameterNode): SignatureNode? {
                    return elem.signature
                        ?.let(SignatureNode::from)
                        ?.also { output -> ctx.scopeTransition(elem, output) }
                }

                return input
                    .mapNotNull(::signatureOf)
            }
        }

        internal data class FilterType(val type: AsmType) : IsoAction<ParameterNode>() {
            override fun id() = "filter-type(${type.simpleName})"
            override fun execute(ctx: Context, input: IterParameters): IterParameters {
                return input.filterBy(ParameterNode::type, type)
            }
        }

        class ExplodeType(val synthesize: Boolean = false): Action<IterParameters, IterClasses>() {
            override fun id() = "explode-type(${"synthesize".takeIf { synthesize } ?: ""})"
            override fun execute(ctx: Context, input: IterParameters): IterClasses {
               fun explode(param: ParameterNode): ClassNode? {

                   var exploded = ctx.classByType[param.type]
                   if (exploded == null && synthesize)
                       exploded = ctx.synthesize(param.type)

                   return exploded
                       ?.also { ctx.scopeTransition(param, it) }
               }

                return input.mapNotNull(::explode)
            }
        }

        internal object ReadType : Action<IterParameters, IterValues>() {
            override fun id() = "read-type"
            override fun execute(ctx: Context, input: IterParameters): IterValues {
                return input
                    .map { ValueNode.from(Type.from(it.type), it) }
                    .onEach { ctx.scopeTransition(it.reference, it) }
            }
        }

        internal object IntoOuterScope : Action<IterParameters, IterMethods>() {
            override fun id() = "outer"
            override fun execute(ctx: Context, input: IterParameters): IterMethods {
                return input
                    .map { p -> p.owner.also { ctx.scopeTransition(p, it) } }
                    .toSet()
            }
        }

        internal data class FilterNth(val nth: Int) : Action<IterParameters, IterParameters>() {
            override fun id() = "filter-nth($nth)"
            override fun execute(ctx: Context, input: IterParameters): IterParameters {
                return input.filter { pn -> pn.owner.parameters.indexOf(pn) == nth }
            }
        }

        internal data class Filter(val regex: Regex, val invert: Boolean) : IsoAction<ParameterNode>() {
            override fun id() = "filter($regex${", invert".takeIf { invert } ?: ""})"
            override fun execute(ctx: Context, input: IterParameters): IterParameters {
                val f = if (invert) input::filterNot else input::filter
                return f { regex in it.name }
            }
        }
    }

    internal data class DebugLog<T : Element>(
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
                        if (elements.first() is ClassNode) "es" else "s"
            }.let(::println)

            return input
        }

        enum class LogFormat {
            Elements, Count
        }
    }

    internal data class HasAnnotation<T : Element>(val annotation: AsmType) : IsoAction<T>() {
        override fun id() = "annotated-by(${annotation.simpleName})"
        override fun execute(ctx: Context, input: Iter<T>): Iter<T> {
            return input
                .filter { annotation in it.annotations.map(AnnotationNode::type) }
        }
    }

    internal data class EntityFilter<T : Element>(val entity: Entity.Type) : IsoAction<T>() {
        override fun id() = "filter-entity($entity)"
        override fun execute(ctx: Context, input: Iter<T>): Iter<T> {
            return input.filter { it in ctx.entityService }
        }
    }

    internal data class FilterModifiers<T : Element>(
        val modifiers: Int,
        val invert: Boolean,
    ) : IsoAction<T>() {
        override fun id() = "filter-modifiers(${"!".takeIf { invert } ?: ""}0x${modifiers.hex(2)})"
        override fun execute(ctx: Context, input: Iter<T>): Iter<T> {
            return input.filter { elem ->
                when (elem) {
                    is ClassNode  -> elem.access
                    is FieldNode  -> elem.access
                    is MethodNode -> elem.access
                    else -> error("No access modifiers possible for: ${elem::class.simpleName}")
                }.let { access ->
                    (access and modifiers == modifiers) xor invert
                }
            }
        }
    }

    class ReadName<T : Element>(val shortened: Boolean = false) : Action<Iter<T>, IterValues>() {
        override fun id() = "read-name"
        override fun execute(ctx: Context, input: Iter<T>): IterValues {
            fun nameOf(elem: T): String = when (elem) {
                is MethodNode    -> elem.name
                is FieldNode     -> elem.name
                is ParameterNode -> elem.name
                is ClassNode     -> elem.innerName?.takeIf { shortened } ?: elem.simpleName
                is SignatureNode -> elem.simpleName
                else             -> error("$elem")
            }

            return input
                .map { ValueNode.from(nameOf(it), it) }
                .onEach { ctx.scopeTransition(it.reference, it) }
        }
    }

    internal data class ReadAnnotation<T: Element>(val annotation: AsmType, val field: String) : Action<Iter<T>, IterValues>() {
        override fun id() = "read-annotation(${annotation.simpleName}::$field)"
        override fun execute(ctx: Context, input: Iter<T>): IterValues {
            fun readAnnotation(input: T): ValueNode? {
                return input.annotations
                    .find { an -> an.type == annotation }
                    ?.let { an -> an[field] }
                    ?.let { ValueNode.from(it, input) }
                    ?.also { ctx.scopeTransition(input, it) }
            }
            return input.mapNotNull(::readAnnotation)
        }
    }

    class WithValue<T : Element>(val value: Any) : Action<Iter<T>, IterValues>() {
        override fun id() = "with-value($value)"
        override fun execute(ctx: Context, input: Iter<T>): IterValues {
            return input
                .map { ValueNode.from(value, it) }
//                .onEach { ctx.scopeTransition(it.reference, it) } // can never refer to value nodes anyway
        }

        override fun toString() = "WithValue($value)"
    }

    class EditText<T : Element> internal constructor(
        private val editor: StringEditor
    ) : Action<Iter<T>, IterValues>() {
        override fun id() = "edit-text($editor)"
        override fun execute(ctx: Context, input: Iter<T>): IterValues {
            return input
                .map { ValueNode.from(editor, it) }
                .onEach { ctx.scopeTransition(it.reference, it) }
        }

        override fun toString() = "EditText($editor)"
    }

    internal data class Fork<T, FORK_T>(
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

    internal data class ForkOnEntityExistence<T, FORK_T>(
        val forked: Action<T, FORK_T>,
        val entity: Entity.Type,
        val invert: Boolean
    ) : Action<T, T>() {
        override fun id() = "fork-conditional($entity exists${"-not".takeIf { invert } ?: ""})"
        override fun execute(ctx: Context, input: T): T {
            if ((entity in ctx.entityService) xor invert) {
                ctx.pushMeasurementScope()
                forked(ctx, input)
                ctx.popMeasurementScope()
            }

            return input
        }
    }

    internal data class RegisterEntity<T : Element>(
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

    internal data class RegisterSynthesizedEntity(
        val id: Entity.Type,
        val type: AsmType,
        val labelFormatter: LabelFormatter
    ) : Action<Unit, Unit>() {
        override fun id() = "register-entity-synthesized($id, ${type.simpleName})"
        override fun execute(ctx: Context, input: Unit) {
            ctx.synthesize(type)
                .let { elem -> ctx.register(Entity(id, "N/A"), elem, labelFormatter) }

            return input
        }
    }

    @FlowPreview
    internal data class RegisterChildren<T : Element>(
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

            fun relations(elem: Element, type: Entity.Type): Flow<Entity> {
                val related = ctx.findRelatedEntities(elem, type)
                if (related.isEmpty() && elem !in ctx.entityService) {
                    Throw.entityNotFound(type, elem)
                }

                return related.filter { ctx.entityService[elem] != it }.toSet().asFlow()
            }

            runBlocking(Dispatchers.Default) {
                ctx.entityService[parentType].entries.chunked(5).asFlow()
                    .flatMapConcat { it.asFlow() }
                    .flatMapConcat { (elem, parent) -> relations(elem, childType).map { parent to it } }
                    .toList()
                    .onEach { (parent, child) -> child.addChild("backtrack", parent) }
                    .onEach { (parent, child) -> parent.addChild(key, child) }
                    .takeIf(List<Pair<Entity, Entity>>::isNotEmpty)
                    ?: Throw.unableToResolveParentRelation(parentType, childType)
            }

            return input
        }
    }

    internal data class RegisterChildrenFromResolver(
        val parentType: Entity.Type,
        val key: String,
        val childResolver: EntityAssignmentResolver<MethodNode>,
    ) : IsoAction<MethodNode>() {
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

    internal data class UpdateEntityProperty(
        val strategy: PropertyStrategy,
        val key: String,
        /** if null: resolves [Entity.Type] from currently referenced element */
        val entity: Entity.Type? = null
    ) : IsoAction<ValueNode>() {
        override fun id() = "update-property($key${", $entity".takeIf { entity != null } ?: ""})"
        override fun execute(ctx: Context, input: IterValues): IterValues {
            when (entity) {
                null -> {
                    input.map { it to ctx.entityService[it.reference] }
                        .forEach { (elem, e) -> e?.set(key, elem.data) }
                }
                else -> {
                    runBlocking(Dispatchers.Default) {
                        input.asFlow()
                            .map { elem ->
                                ctx.findRelatedEntities(elem, entity)
                                    .associateWith { elem.data.ensureList }
                            }
                            .toList()
                            .takeIf { it.isNotEmpty() }
                            ?.reduce { acc, f -> acc + f }
                            ?.let(::updateProperties)
                    }
                }
            }

            return input
        }

        private operator fun Map<Entity, List<Any>>.plus(rhs: Map<Entity, List<Any>>): Map<Entity, List<Any>> {
            return toMutableMap().also { out ->
                rhs.forEach { (k, v) -> out[k] = out[k]?.plus(v) ?: v }
            }
        }

        private fun updateProperties(entities: Map<Entity, List<Any>>) {
            when (strategy) {
                PropertyStrategy.replace -> entities.forEach { (e, data) ->
                    e -= key
                    e[key] = data
                }
                PropertyStrategy.append -> entities.forEach { (e, data) ->
                    e[key] = data
                }
                PropertyStrategy.prepend -> entities.forEach { (e, data) ->
                    val existing = e[key] ?: listOf()
                    e -= key
                    e[key] = data + existing
                }
                PropertyStrategy.immutable -> entities.forEach { (e, data) ->
                    if (key !in e.properties) {
                        e[key] = data
                    }
                }
                PropertyStrategy.unique -> entities.forEach { (e, data) ->
                    e[key] = data.filter { it !in e[key]!! }
                }
            }
        }
    }

    internal data class Compose<A, B, C>(
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

        internal abstract fun invoke(ctx: Context, input: T): T?
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

private fun Int.hex(bytes: Int): String = toUInt().toString(16).padStart(bytes * 2, '0')

internal fun <T> chainFrom(action: Action<T, T>) = Action.Chain(mutableListOf(action))



var debugLog = false

infix fun Type.matches(types: List<Type>): Boolean {
    return this in (types.takeIf { isGeneric } ?: types.map { Type.from(it.asmType) })
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified T : Element> Context.entities(
    type: Entity.Type
): Map<T, Entity> {
    val entitiesByType: Map<Element, Entity> = entityService[type]
    if (entitiesByType.isNotEmpty()) {
        val first = entitiesByType.keys.first()
        if (first !is T) {
            throw UnexpectedElementException(T::class, first::class)
        }
    }

    return entitiesByType as Map<T, Entity>
}


@Suppress("UNCHECKED_CAST")
private val Any.ensureList: List<Any>
    get() = if (this is List<*>) this as List<Any> else listOf(this)