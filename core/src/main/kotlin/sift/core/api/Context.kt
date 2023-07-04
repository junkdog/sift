package sift.core.api

import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.TextStyles.bold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import net.onedaybeard.collectionsby.filterBy
import net.onedaybeard.collectionsby.findBy
import sift.core.SynthesisTemplate
import sift.core.Throw.entityTypeAlreadyBoundToElementType
import sift.core.api.MeasurementScope.Template
import sift.core.asm.classNode
import sift.core.asm.signature.ArgType
import sift.core.asm.signature.TypeSignature
import sift.core.dsl.Type
import sift.core.dsl.type
import sift.core.element.*
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.entity.LabelFormatter
import sift.core.terminal.Gruvbox
import sift.core.tree.Tree
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

internal data class Context(
    val allClasses: MutableList<ClassNode>
) {
    constructor(
        cns: Iterable<AsmClassNode>
    ) : this(cns.map(ClassNode::from).toMutableList())

    val entityService: EntityService = EntityService()
    var elementTraces: MutableMap<Element, MutableList<ElementTrace>> = ConcurrentHashMap()

    private val labelFormatters: MutableMap<Entity, LabelFormatter> = mutableMapOf()

    val classByType: MutableMap<Type, ClassNode> = allClasses
        .associateBy(ClassNode::type)
        .toMutableMap()

    private val methodInvocationsCache: MutableMap<MethodNode, Iterable<MethodNode>> = ConcurrentHashMap()
    private val methodFieldAccessCache: MutableMap<MethodNode, Iterable<FieldNode>> = mutableMapOf()

    val parents: MutableMap<ClassNode, List<TypeClassNode>> = allClasses
        .associateWith(classByType::parentsTypesOf)
        .toMutableMap()
    val implementedInterfaces: MutableMap<ClassNode, List<TypeClassNode>> = allClasses
        .associateWith { cn ->

            val found = mutableSetOf<TypeClassNode>()
            fun recurse(node: ClassNode) {
                val interfaces = node.signature?.implements?.map(TypeSignature::toType)
                    ?: node.interfaces

                interfaces
                    .map { TypeClassNode(it, classByType[it]) }
                    .filter { it !in found }
                    .onEach { found += it }
                    .mapNotNull(TypeClassNode::cn)
                    .forEach(::recurse)

                parents[node]?.let { parents ->
                    parents
                        .onEach(found::add)
                        .mapNotNull(TypeClassNode::cn)
                        .onEach(::recurse)
                }
            }
            recurse(cn)

            found.toList()
        }
        .toMutableMap()


    val measurements: Tree<Measurement> = Tree(Measurement(".", Template, Template, 0, 0, 0, 0.milliseconds))
    private var measurementStack: MutableList<Tree<Measurement>> = mutableListOf(measurements)
    private var pushScopes: Int = 0

    fun synthesize(type: Type): ClassNode {
        return classByType[type] ?: (classNode<SynthesisTemplate>()
            .also { cn -> cn.name = type.internalName }
            .let(ClassNode::from)
            .apply {
                allClasses += this
                implementedInterfaces[this] = listOf()
                parents[this] = listOf()
                classByType[type] = this
        })
    }

    fun methodsInvokedBy(mn: MethodNode): Iterable<MethodNode> {
        return methodInvocationsCache.getOrPut(mn) { methodsInvokedBy(mn, classByType) }
    }

    fun fieldAccessBy(mn: MethodNode): Iterable<FieldNode> {
        return methodFieldAccessCache.getOrPut(mn) {
            fieldAccessBy(methodsInvokedBy(mn), classByType)
        }
    }

    private var lock = Any()

    fun synthesize(owner: Type, name: String, desc: String): MethodNode {
        val cn = classByType[owner]
            ?: error("'${owner}' not found")

        // if method already exists there's no need to synthesize it
        val mn = cn.methods
            .filterBy(MethodNode::name, name)
            .filterBy(MethodNode::desc, desc)
            .also { mns -> require(mns.size < 2) { error("$mns") } }
            .firstOrNull()

        return mn ?: AsmMethodNode().also { method -> // stub
            method.name = name
            method.desc = desc
        }.let { method -> MethodNode.from(cn, method, null) }.also {
            method ->
                // register
            synchronized(lock) {
                cn.methods.add(method)

                // must rebuild method invocation cache
                methodInvocationsCache.clear()
            }
        }
    }

    private val bufferedTransitions: MutableList<Pair<Element, Element>> = mutableListOf()

    fun scopeTransition(input: Element, output: Element) {
        bufferedTransitions += input to output
    }

    private fun flushTransitions() {
        when (bufferedTransitions.size) {
            0       -> return
            in 1..4 -> bufferedTransitions.forEach { (input, output) -> registerTransition(input, output) }
            else    -> runBlocking(Dispatchers.Default) {
                bufferedTransitions.asFlow()
                    .map { (input, output) -> registerTransition(input, output) }
                    .collect()
            }
        }

        bufferedTransitions.clear()
    }

    private fun registerTransition(input: Element, output: Element) {
        // existing traces of element being scoped to
        val currentTraces = tracesOf(output)

        // resolve new traces; filter already scoped element traces
        val transitions = tracesOf(input)
            .map { it + output }
            .filter { it !in currentTraces }

        currentTraces
            .also { trails -> trails.removeAll { o -> transitions.any { it in o } } }
            .addAll(transitions)
    }

    fun allInterfacesOf(cn: ClassNode, includeParents: Boolean = true): List<Type> {
        val allImplemented = implementedInterfaces[cn] ?: listOf()

        return when {
            includeParents -> allImplemented
            else -> {
                val parents = (parents[cn] ?: listOf())
                allImplemented - parents
            }
        }.map(TypeClassNode::type)
    }

    private fun resolveTracedElement(input: Element): Element {
        // to avoid excessive resource consumption, we avoid bookkeeping
        // traces of elements that are intrinsically tied to other (traced) elements.
        return (input as? ValueNode)?.reference?.let(::resolveTracedElement)
            ?: (input as? AnnotationNode)?.root
            ?: input
    }

    fun findRelatedEntities(input: Element, entity: Entity.Type): Set<Entity> {
        val input = resolveTracedElement(input)

        // the most immediate path back to the root element
        val plain = tracesOf(input)
            .mapNotNull { entityService.filter(it, entity) }
            .toSet()

        // check if input element is contained in the trails of eligible entities
        val reverse = entityService[entity]
            .map { (elem, e) -> e to tracesOf(elem) }
            .asSequence()
            .flatMap { (e, trails) -> trails.map { e to it } }
            .filter { (_, trail) -> input in trail }
            .map { (e, _) -> e }
            .toSet()

        return plain + reverse
    }

    fun register(entity: Entity, element: Element, formatter: LabelFormatter) {
        // if entity type is already registered, ensure its element type is the same
        if (entity.type in entityService) {
            val elementType = entityService[entity.type]
                .asSequence()
                .first()
                .key::class

            if (element::class != elementType) {
                entityTypeAlreadyBoundToElementType(entity.type, elementType, element::class)
            }
        }

        // updates label+properties on re-registration
        entityService.register(entity, element)
            .let { e -> labelFormatters[e] = formatter }
    }

    fun tracesOf(element: Element): MutableList<ElementTrace> {
        return elementTraces.getOrPut(element) { mutableListOf(ElementTrace(element)) }
    }

    fun updateEntityLabels() {
        entityService.allEntities().forEach { e ->
            e.label = labelFormatters[e]!!.format(e, entityService)
        }
    }

    fun <IN, OUT> measure(ctx: Context, input: IN, action: Action<IN, OUT>): OUT {

        fun <T> sizeOf(any: T): Int = when (any) {
            is Iterable<*> -> any.toList().size
            is Unit        -> 0
            else           -> error("halp: $any")
        }

        val measurement = Measurement(
            action = action.id(),
            scopeIn = MeasurementScope.FromContext,
            scopeOut = MeasurementScope.FromContext,
            input = sizeOf(input),
            output = 0,
            entites = 0,
            execution = 0.seconds
        )
        when (action) {
            is Action.Compose<*, *, *> -> Unit
            is Action.Chain<*>         -> Unit
            else -> {
                if (pushScopes > 0) {
                    measurementStack += measurementStack.last().children().last() //.add(measurement)
                    measurementStack.last().add(measurement)
                    pushScopes--
                } else {
                    measurementStack.last().add(measurement)
                }
            }
        }

        val start = System.nanoTime().nanoseconds
        val out = action.execute(ctx, input) // TODO: measureTimedValue
        ctx.flushTransitions()
        val end = System.nanoTime().nanoseconds
        measurement.apply {
            output = sizeOf(out)
            execution = end - start
            entites = ctx.entityService.allEntities().size
        }

        return out
    }

    fun pushMeasurementScope() {
        pushScopes++
    }

    fun popMeasurementScope() {
        measurementStack.removeLast()
    }

    fun statistics(): Map<String, Int> = mapOf(
        "allClasses"                     to allClasses.size,
        "associations.keys"              to elementTraces.size,
        "associations.traces"            to elementTraces.values.flatten().size,
        "associations.traces.max"        to elementTraces.values.maxOf(List<ElementTrace>::size),
        "associations.traces.med"        to elementTraces.values.medianOf(List<ElementTrace>::size),
        "associations.traces.depth.max"  to elementTraces.values.flatten().maxOf { it.asIterable().count() },
        "associations.traces.depth.med"  to elementTraces.values.flatten().medianOf { it.asIterable().count() },
        "associations.flatten"           to elementTraces.values.flatten().sumOf { it.asIterable().count() },
        "classByType"                    to classByType.size,
        "methodInvocationsCache"         to methodInvocationsCache.size,
        "methodInvocationsCache.flatten" to methodInvocationsCache.values.sumOf(Iterable<MethodNode>::count),
        "methodFieldCache"               to methodFieldAccessCache.size,
        "methodFieldCache.flatten"       to methodFieldAccessCache.values.sumOf(Iterable<FieldNode>::count),
        "parents"                        to parents.size,
        "parents.flatten"                to parents.values.sumOf(Iterable<TypeClassNode>::count),
        "implementedInterfaces"          to implementedInterfaces.size,
        "implementedInterfaces.flatten"  to implementedInterfaces.values.sumOf(Iterable<TypeClassNode>::count),
    )
}

inline fun <T> Iterable<T>.medianOf(
    selector: (T) -> Int
): Int = with (map(selector)) { sorted()[size / 2] }

private fun TypeSignature.toType(): Type {
    val generics = args
        .takeIf(List<TypeSignature>::isNotEmpty)
        ?.map(TypeSignature::toType)
        ?.joinToString(prefix = "<", postfix = ">")
        ?: ""

    return "${type.className()}$generics".type
}

private fun ArgType.className(): String {
    return when (this) {
        is ArgType.Array -> wrapped?.className() + "[]"
        is ArgType.Plain -> type.internalName
        is ArgType.Var   -> type.name
    }
}

internal fun Context.coercedMethodsOf(type: Entity.Type): Map<MethodNode, Entity> {
    fun toMethodNodes(elem: Element, e: Entity): List<Pair<MethodNode, Entity>> {
        return when (elem) {
            is ClassNode     -> elem.methods.map { mn -> mn to e }
            is MethodNode    -> listOf(elem to e)
            is ParameterNode -> listOf(elem.owner to e)
            is ValueNode     -> toMethodNodes(elem.reference, e)
            else             -> error("unable to extract methods from $elem")
        }
    }

    return entityService[type]
        .flatMap { (elem, e) -> toMethodNodes(elem, e) } // FIXME: throw
        .toMap()
}

private val ClassNode.parentType: Type?
    get() = extends?.let(TypeSignature::toType) ?: superType

private fun Map<Type, ClassNode>.parentsTypesOf(cn: ClassNode): List<TypeClassNode> {
    fun Map<Type, ClassNode>.next(tcn: TypeClassNode): TypeClassNode? {
        val parentType = tcn.cn?.parentType ?: return null
        val parent = get(parentType.rawType)
        return TypeClassNode(parentType, parent)
    }

    return generateSequence(TypeClassNode(cn.type, cn)) { tcn -> next(tcn) }
        .drop(1)
        .toList()
}

internal fun Map<Type, ClassNode>.parentsOf(cn: ClassNode): List<ClassNode> {
    return generateSequence(cn) { get(it.superType) }
        .drop(1)
        .toList()
}

data class Measurement(
    var action: String,
    var scopeIn: MeasurementScope,
    var scopeOut: MeasurementScope,
    var input: Int,
    var output: Int,
    var entites: Int,
    var execution: Duration
) {
    companion object {
        val NONE: Measurement = Measurement(
            "none", MeasurementScope.FromContext, MeasurementScope.FromContext, 0, 0, 0, 0.seconds
        )
    }
}

enum class MeasurementScope(val id: String) {
    Annotation("annotation-scope"),
    Template("template-scope"),
    Signature("signature-scope"),
    Class("class-scope"),
    Field("field-scope"),
    Method("method-scope"),
    Parameter("parameter-scope"),
    TypeErased("element-scope"),
    Exception("exception-scope"), // used as marker when an exception is thrown
    FromContext("")
}

private fun EntityService.filter(
    trail: ElementTrace,
    entity: Entity.Type
): Entity? = trail
    .mapNotNull { this[it] }
    .findBy(Entity::type, entity)

internal fun Context.debugTrails() {
    val colWidth = elementTraces.flatMap { (_, trails) -> trails }
        .flatMap(ElementTrace::toList)
        .map(Element::toString)
        .maxOfOrNull(String::length) ?: 0

    elementTraces.flatMap { (_, trails) -> trails }
        .map { it.debugString(this, colWidth) }
        .forEach(::println)
}

private fun ElementTrace.debugString(
    context: Context,
    colWidth: Int = 20
): String {
    return joinToString(" ") { e ->
        if (e in context.entityService) {
            TextStyles.inverse(e.stylized(colWidth))
        } else {
            e.stylized(colWidth)
        }
    }
}

private fun Element.stylized(width: Int): String {
    val s = toString()
    val lastIndex = minOf(width - 1, s.lastIndex)

    return (when (this) {
        is AnnotationNode -> Gruvbox.aqua2
        is ClassNode      -> Gruvbox.yellow2
        is FieldNode      -> Gruvbox.purple2
        is MethodNode     -> Gruvbox.green2
        is ParameterNode  -> Gruvbox.orange2
        is SignatureNode  -> Gruvbox.blue2
        is ValueNode      -> Gruvbox.gray245
    } + bold)(s.substring(0..lastIndex).padEnd(width))
}

internal data class TypeClassNode(
    val type: Type,
    val cn: ClassNode?
)