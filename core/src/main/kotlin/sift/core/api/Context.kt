package sift.core.api

import net.onedaybeard.collectionsby.filterBy
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import sift.core.SynthesisTemplate
import sift.core.api.MeasurementScope.Instrumenter
import sift.core.asm.classNode
import sift.core.asm.type
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
    val classByMethod: IdentityHashMap<MethodNode, ClassNode> = allClasses
        .flatMap { cn -> cn.methods.map { mn -> mn to cn } }
        .toMap()
        .let(::IdentityHashMap)

    internal val entityService: EntityService = EntityService()
    var trails: MutableMap<Element, MutableList<ElementTrail>> = mutableMapOf()

    private val labelFormatters: MutableMap<Entity, LabelFormatter> = mutableMapOf()

    val classByType: MutableMap<Type, ClassNode> = allClasses
        .associateBy(ClassNode::type)
        .toMutableMap()

    private val methodInvocationsCache: MutableMap<MethodNode, Iterable<MethodNode>> = mutableMapOf()

    val parents: MutableMap<ClassNode, List<ClassNode>> = allClasses
        .associateWith(allClasses::parentsOf)
        .toMutableMap()
    val implementedInterfaces: MutableMap<ClassNode, List<Type>> = allClasses
        .associateWith { cn ->

            val found = mutableSetOf<Type>()
            fun recurse(node: ClassNode) {
                interfacesOf(node)
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

    init {
        allClasses.associateWith { cn ->
            val cns = (parents[cn] ?: listOf()) + cn
            cns.map(::interfacesOf)
        }
    }

    fun synthesize(type: Type): ClassNode {
        return classByType[type] ?: classNode<SynthesisTemplate>().apply {
            name = type.internalName
            allClasses += this
            implementedInterfaces[this] = listOf()
            parents[this] = listOf()
            classByType[type] = this
        }
    }

    fun methodsInvokedBy(mn: MethodNode): Iterable<MethodNode> {
        return methodInvocationsCache.getOrPut(mn) { methodsInvokedBy(mn, classByType) }
    }

    fun synthesize(owner: Type, name: String, desc: String): MethodNode {
        val cn = classByType[owner]
            ?: error("'${owner}' not found")

        // if method already exists there's no need to synthesize it
        val mn = cn.methods
            .filterBy(MethodNode::name, name)
            .filterBy(MethodNode::desc, desc)
            .also { mns -> require(mns.size < 2) { error("$mns") } }
            .firstOrNull()

        return mn ?: MethodNode().also { method ->
            // stub
            method.name = name
            method.desc = desc

            // register
            cn.methods.add(method)
            classByMethod[method] = cn

            // must rebuild method invocation cache
            methodInvocationsCache.clear()
        }
    }

    fun scopeTransition(input: Element, output: Element) {
        // TODO: ensure no duplicates
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

    fun trailsOf(element: Element): MutableList<ElementTrail> {
        return trails.getOrPut(element) { mutableListOf(ElementTrail(element)) }
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
    trail: ElementTrail,
    entity: Entity.Type
): Entity? = trail
    .mapNotNull { this[it] }
    .find { it.type == entity }
