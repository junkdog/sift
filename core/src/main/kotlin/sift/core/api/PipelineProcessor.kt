package sift.core.api

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.objectweb.asm.tree.ClassNode
import sift.core.entity.Entity
import sift.core.jackson.SystemModelSerializer
import sift.core.tree.Tree
import kotlin.time.Duration.Companion.nanoseconds

class PipelineProcessor(classNodes: Iterable<ClassNode>) {
    private val context: Context = Context(classNodes.toMutableList())

    internal fun processPipeline(action: Action<Unit, Unit>, profile: Boolean): Context {
        val start = System.nanoTime()
        action(context, Unit)
        context.updateEntityLabels()
        val end = System.nanoTime()

        if (profile) {
            // tag scopes
            context.measurements
                .walk()
                .filter { "-scope" in it.value.action }
                .forEach { node ->
                    enumValues<MeasurementScope>()
                        .first { it.id == node.value.action }
                        .let { scope -> node.value.scopeIn = scope }
                }

            // mark scopes
            context.measurements
                .walk()
                .forEach { node ->
                    node.value.scopeIn = generateSequence(node, Tree<Measurement>::prev)
                        .first { it.value.scopeIn != MeasurementScope.FromContext }
                        .value
                        .scopeIn

                    node.prev?.value?.scopeOut = node.value.scopeIn

                    if (node.children().isNotEmpty())
                        node.value.execution = (-1).nanoseconds
                }

            context.measurements.value.let { root ->
                root.execution = (end - start).nanoseconds
                root.action = "pipeline"
            }
        }

        return context
    }

    fun execute(action: Action<Unit, Unit>, profile: Boolean): SystemModel {
        return processPipeline(action, profile).let { ctx -> SystemModel(ctx, ctx.measurements) }
    }
}

@JsonSerialize(using = SystemModelSerializer.Serializer::class)
@JsonDeserialize(using = SystemModelSerializer.Deserializer::class)
data class SystemModel(
    val entitiesByType: Map<Entity.Type, List<Entity>>,
    val measurements: Tree<Measurement>,
) {
    internal constructor(
        context: Context,
        measurements: Tree<Measurement>
    ) : this(
        context.entityService.entitiesByType.map { (type, v) -> type to v.values.toList() }.toMap(),
        measurements,
    )

    operator fun get(type: Entity.Type): List<Entity> = entitiesByType[type] ?: listOf()
    operator fun contains(type: Entity.Type): Boolean = type in entitiesByType
}