package sift.core.api

import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import net.onedaybeard.collectionsby.filterBy
import sift.core.SynthesisTemplate
import sift.core.Throw.entityTypeAlreadyBoundToElementType
import sift.core.api.MeasurementScope.Template
import sift.core.asm.classNode
import sift.core.asm.signature.TypeSignature
import sift.core.dsl.Type
import sift.core.element.*
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.entity.LabelFormatter
import sift.core.template.DeserializedSystemModelTemplate
import sift.core.terminal.Gruvbox
import sift.core.topologicalSort
import sift.core.tree.Tree
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

internal data class Context(
    val allClasses: MutableList<ClassNode>,
    var profiling: Boolean = false
) {
    val entityService: EntityService = EntityService()
    val elementAssociations = ElementTraceRegistry(entityService)

    private val labelFormatters: MutableMap<Entity, LabelFormatter> = mutableMapOf()

    val classByType: MutableMap<Type, ClassNode> = allClasses
        .associateBy(ClassNode::type)
        .toMutableMap()

    private val methodInvocationsCache: MutableMap<MethodNode, Iterable<MethodNode>> = ConcurrentHashMap()
    private val methodFieldAccessCache: MutableMap<MethodNode, Iterable<FieldNode>> = mutableMapOf()

    private val injectedClasses: MutableList<ClassNode> = mutableListOf()

    val parents: MutableMap<ClassNode, List<TypeClassNode>> = allClasses
        .associateWith(classByType::parentsTypesOf)
        .toMutableMap()
    var implementedInterfaces: Map<ClassNode, List<TypeClassNode>> = allClasses
        .associateWith { cn -> interfacesOf(cn) }

    private fun interfacesOf(cn: ClassNode): List<TypeClassNode> {
        val found = mutableSetOf<TypeClassNode>()
        fun recurseInterfaces(node: ClassNode) {
            val interfaces = node.signature?.implements?.map(TypeSignature::toType)
                ?: node.interfaces

            interfaces
                .map { TypeClassNode(it, classByType[it.rawType]) }
                .filter { it !in found }
                .onEach { found += it }
                .mapNotNull(TypeClassNode::cn)
                .forEach(::recurseInterfaces)
        }

        fun recurseSuperclasses(node: ClassNode) {
            parents[node]?.let { parents ->
                parents
                    .onEach(found::add)
                    .mapNotNull(TypeClassNode::cn)
                    .onEach(::recurseSuperclasses)
                    .onEach(::recurseInterfaces)
            }
        }

        recurseInterfaces(cn)
        recurseSuperclasses(cn)

        return found.toList()
    }

    val measurements: Tree<Measurement> = Tree(Measurement(".", Template, Template, 0, 0, 0, 0.milliseconds))
    private var measurementStack: MutableList<Tree<Measurement>> = mutableListOf(measurements)
    private var pushScopes: Int = 0

    fun inject(cn: ClassNode) {
        injectedClasses += cn
    }

    // NOP unless popping a synthesis scope with inject(cls) calls
    fun flushInjectedClasses() {
        if (injectedClasses.isEmpty())
            return

        val typeToCn = injectedClasses.associateBy(ClassNode::type)

        // classes are injected in an order that respects their dependencies
        injectedClasses.topologicalSort { cn ->
            listOfNotNull(
                cn.superType?.let { typeToCn[it] },
                *cn.interfaces.mapNotNull { typeToCn[it] }.toTypedArray()
            )
        }.forEach(::registerClass)

        methodInvocationsCache.clear()
        injectedClasses.clear()

        implementedInterfaces = allClasses.associateWith { cn -> interfacesOf(cn) }
    }

    fun synthesize(type: Type): ClassNode {
        return classByType[type] ?: (classNode<SynthesisTemplate>()
            .also { cn -> cn.name = type.internalName }
            .let(ClassNode::from)
            .also { cn -> registerClass(cn) }
        )
    }

    private fun registerClass(
        cn: ClassNode,
    ) {
        allClasses += cn
        classByType[cn.type] = cn
        parents[cn] = classByType.parentsTypesOf(cn)
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
        bufferedTransitions
            .forEach { (input, output) -> elementAssociations.registerTransition(input, output) }

        bufferedTransitions.clear()
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

    fun findRelatedEntities(input: Element, entity: Entity.Type): Set<Entity> {
        return elementAssociations.findRelatedEntities(input, entity)
    }

    fun register(entity: Entity, element: Element, formatter: LabelFormatter) {
        val sanitized = elementAssociations.register(entity, element)

        // if entity type is already registered, ensure its element type is the same
        if (entity.type in entityService) {
            val elementType = entityService[entity.type]
                .asSequence()
                .first()
                .key::class

            if (sanitized::class != elementType) {
                entityTypeAlreadyBoundToElementType(entity.type, elementType, sanitized::class)
            }
        }

        // updates label+properties on re-registration
        entityService.register(entity, sanitized)
            .let { e -> labelFormatters[e] = formatter }
    }

    fun updateEntityLabels() {
        entityService.allEntities().forEach { e ->
            e.label = labelFormatters[e]!!.format(e, entityService)
        }
    }

    fun <IN, OUT> execute(input: IN, action: Action<IN, OUT>): OUT = when {
        profiling -> profiledExecute(input, action)
        else      -> action.execute(this, input).also { flushTransitions() }
    }

    private fun <IN, OUT> profiledExecute(input: IN, action: Action<IN, OUT>): OUT {
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
        val out = action.execute(this, input)
        flushTransitions()
        val end = System.nanoTime().nanoseconds
        measurement.apply {
            output = sizeOf(out)
            execution = end - start
            entites = entityService.allEntities().size
        }

        return out
    }

    fun pushMeasurementScope() {
        pushScopes++
    }

    fun popMeasurementScope() {
        if (profiling)
            measurementStack.removeLast()
    }

    fun statistics(): Map<String, Int> = mapOf(
        "timing.ms.deserializeTemplate"   to DeserializedSystemModelTemplate.deserializationTime.inWholeMilliseconds.toInt(),
        "timing.ms.parseAsmClassNodes"    to stats.parseAsmClassNodes.inWholeMilliseconds.toInt(),
        "timing.ms.parseSiftClassNodes"   to stats.parseSiftClassNodes.inWholeMilliseconds.toInt(),
        "timing.ms.templateProcessing"    to stats.templateProcessing.inWholeMilliseconds.toInt(),
        "allClasses"                      to allClasses.size,
        "classByType"                     to classByType.size,
        "methodInvocationsCache"          to methodInvocationsCache.size,
        "methodInvocationsCache.flatten"  to methodInvocationsCache.values.sumOf(Iterable<MethodNode>::count),
        "methodFieldCache"                to methodFieldAccessCache.size,
        "methodFieldCache.flatten"        to methodFieldAccessCache.values.sumOf(Iterable<FieldNode>::count),
        "parents"                         to parents.size,
        "parents.flatten"                 to parents.values.sumOf(Iterable<TypeClassNode>::count),
        "implementedInterfaces"           to implementedInterfaces.size,
        "implementedInterfaces.flatten"   to implementedInterfaces.values.sumOf(Iterable<TypeClassNode>::count),
    ) + elementAssociations.statistics()

    data class Statistics(
        var parseAsmClassNodes: Duration = 0.milliseconds,
        var parseSiftClassNodes: Duration = 0.milliseconds,
        var templateProcessing: Duration = 0.milliseconds,
    )

    val stats = Statistics()

    companion object {
        @OptIn(ExperimentalTime::class)
        fun from(classes: Iterable<AsmClassNode>): Context {
            val (cns, ms) = measureTimedValue { mapClassNodes(classes) }
            return Context(cns.toMutableList())
                .also { it.stats.parseSiftClassNodes = ms }

        }

        private fun mapClassNodes(cns: Iterable<AsmClassNode>): List<ClassNode> = runBlocking {
            cns.asFlow()
                .map(ClassNode::from)
                .toList()
        }
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
    get() = extends?.type ?: superType

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

internal fun Context.debugTrails() {
    val colWidth = elementAssociations.allTraces()
        .flatten()
        .map(Element::toString)
        .maxOfOrNull(String::length) ?: 0

    elementAssociations.allTraces()
        .map { it.debugString(this, colWidth) }
        .forEach(::println)
}

private fun List<Element>.debugString(
    context: Context,
    colWidth: Int = 20
): String {
    return joinToString(" ") { e ->
        if (e in context.entityService) {
            inverse(e.stylized(colWidth))
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