package sift.core.api

import net.onedaybeard.collectionsby.firstBy
import org.objectweb.asm.tree.ClassNode
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
                        .firstBy(MeasurementScope::id, node.value.action)
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
