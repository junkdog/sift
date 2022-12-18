package sift.core.api

import net.onedaybeard.collectionsby.filterBy
import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.Type
import sift.core.SynthesisTemplate
import sift.core.api.MeasurementScope.Instrumenter
import sift.core.asm.classNode
import sift.core.element.*
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.entity.LabelFormatter
import sift.core.tree.Tree
import java.util.IdentityHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

data class Context(
    val allClasses: MutableList<ClassNode>
) {
    constructor(
        cns: Iterable<AsmClassNode>
    ) : this(cns.map(ClassNode::from).toMutableList())

    val classByMethod: IdentityHashMap<MethodNode, ClassNode> = allClasses
        .flatMap { cn -> cn.methods.map { mn -> mn to cn } }
        .toMap()
        .let(::IdentityHashMap)

    internal val entityService: EntityService = EntityService()
    var trails: MutableMap<Element, MutableList<ScopeTrail>> = mutableMapOf()

    private val labelFormatters: MutableMap<Entity, LabelFormatter> = mutableMapOf()

    val classByType: MutableMap<AsmType, ClassNode> = allClasses
        .associateBy(ClassNode::type)
        .toMutableMap()

    private val methodInvocationsCache: MutableMap<MethodNode, Iterable<MethodNode>> = mutableMapOf()

    val parents: MutableMap<ClassNode, List<ClassNode>> = allClasses
        .associateWith(classByType::parentsOf)
        .toMutableMap()
    val implementedInterfaces: MutableMap<ClassNode, List<AsmType>> = allClasses
        .associateWith { cn ->

            val found = mutableSetOf<AsmType>()
            fun recurse(node: ClassNode) {
                node.interfaces
                    .filter { it !in found }
                    .onEach { found += it }
                    .mapNotNull { classByType[it] }
                    .forEach(::recurse)

                parents[node]
                    ?.onEach(::recurse)
                    ?.onEach { found += it.type }
            }
            recurse(cn)

            found.toList()
        }
        .toMutableMap()


    val measurements: Tree<Measurement> = Tree(Measurement(".", Instrumenter, Instrumenter, 0, 0, 0.milliseconds))
    private var measurementStack: MutableList<Tree<Measurement>> = mutableListOf(measurements)
    private var pushScopes: Int = 0

    fun synthesize(type: AsmType): ClassNode {
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

    fun synthesize(owner: AsmType, name: String, desc: String): MethodNode {
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
        }.let { method -> MethodNode.from(cn, method) }.also {
            method ->
                // register
                cn.methods.add(method)
                classByMethod[method] = cn

                // must rebuild method invocation cache
                methodInvocationsCache.clear()
        }
    }

    fun scopeTransition(input: Element, output: Element) {
        trailsOf(output) += trailsOf(input).map { it + output }
    }

    fun allInterfacesOf(cn: ClassNode): List<Type> {
        return implementedInterfaces[cn] ?: listOf()
    }

    fun findRelatedEntities(input: Element, entity: Entity.Type): Set<Entity> {
        return trailsOf(input)
            .mapNotNull { entityService.filter(it, entity) }
            .toSet()
    }

    fun register(entity: Entity, element: Element, formatter: LabelFormatter) {
        entityService.register(entity, element)
        labelFormatters[entity] = formatter
    }

    fun trailsOf(element: Element): MutableList<ScopeTrail> {
        return trails.getOrPut(element) { mutableListOf(ScopeTrail(element)) }
    }

    fun updateEntityLabels() {
        entityService.allEntities().forEach { e ->
            e.label = labelFormatters[e]!!.format(e, entityService)
        }
    }

    internal fun <IN, OUT> measure(ctx: Context, input: IN, action: Action<IN, OUT>): OUT {

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
        val out = action.execute(ctx, input)
        val end = System.nanoTime().nanoseconds
        measurement.output = sizeOf(out)
        measurement.execution = end - start

        return out
    }

    internal fun pushMeasurementScope() {
        pushScopes++
    }

    internal fun popMeasurementScope() {
        measurementStack.removeLast()
    }
}

private fun Iterable<ClassNode>.parentsOf(cn: ClassNode): List<ClassNode> {
    return generateSequence(cn) { findBy(ClassNode::type, it.superType) }
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
    var execution: Duration,
)

enum class MeasurementScope(val id: String) {
    Instrumenter("instrumenter-scope"),
    Signature("signature-scope"),
    Class("class-scope"),
    Field("field-scope"),
    Method("method-scope"),
    Parameter("parameter-scope"),
    FromContext("")
}

private fun EntityService.filter(
    trail: ScopeTrail,
    entity: Entity.Type
): Entity? = trail
    .mapNotNull { this[it] }
    .find { it.type == entity }
