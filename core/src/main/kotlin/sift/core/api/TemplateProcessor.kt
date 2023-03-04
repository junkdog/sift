package sift.core.api

import net.onedaybeard.collectionsby.firstBy
import org.objectweb.asm.tree.ClassNode
import sift.core.tree.Tree
import kotlin.time.Duration.Companion.nanoseconds

class TemplateProcessor(classNodes: Iterable<ClassNode>) {
    private val context: Context = Context(classNodes)

    internal fun process(
        template: Action<Unit, Unit>,
        profile: Boolean,
        onComplete: (Context) -> Unit = {}
    ): Context {
        val start = System.nanoTime()
        template(context, Unit)
        context.updateEntityLabels()
        val end = System.nanoTime()

        onComplete(context)

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
                root.action = "template"
                root.entites = context.entityService.allEntities().size
            }
        }

        return context
    }

    fun execute(template: Action<Unit, Unit>, profile: Boolean): SystemModel {
        return process(template, profile).let(::SystemModel)
    }
}
