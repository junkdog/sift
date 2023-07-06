package sift.core.api

import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.tree.ClassNode
import sift.core.TemplateProcessingException
import sift.core.asm.resolveClassNodes
import sift.core.tree.Tree
import java.net.URI
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class TemplateProcessor(classNodes: Iterable<ClassNode>) {
    private val context: Context = Context.from(classNodes)

    fun execute(template: Action<Unit, Unit>, profile: Boolean): SystemModel {
        return process(template, profile).let(::SystemModel)
    }

    internal fun process(
        template: Action<Unit, Unit>,
        profile: Boolean,
        onComplete: (Context) -> Unit = {}
    ): Context {
        val start = System.nanoTime()
        try {
            template(context, Unit)
        } catch (e: Exception) {
            if (profile)
                updateMeasurements(start, System.nanoTime())

            throw TemplateProcessingException(context.measurements.takeIf { profile }, e)
        }
        context.updateEntityLabels()
        val end = System.nanoTime()
        context.stats.templateProcessing = (end - start).nanoseconds

        onComplete(context)

        if (profile)
            updateMeasurements(start, end)

        return context
    }

    private fun updateMeasurements(
        start: Long,
        end: Long
    ) {
        // tag scopes
        context.measurements
            .walk()
            .filter { "-scope" in it.value.action }
            .forEach { node ->
                enumValues<MeasurementScope>()
                    .findBy(MeasurementScope::id, node.value.action)
                    ?.let { scope -> node.value.scopeIn = scope }
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

    companion object {

        @ExperimentalTime
        fun from(source: String, mavenRepositories: List<URI>): TemplateProcessor {
            val (cns, duration) = measureTimedValue {
                resolveClassNodes(source, mavenRepositories)
            }

            return TemplateProcessor(cns)
                .also { it.context.stats.parseAsmClassNodes = duration }
        }
    }
}
