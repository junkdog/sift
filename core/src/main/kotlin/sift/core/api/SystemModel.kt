package sift.core.api

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import sift.core.asm.resolveClassNodes
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.jackson.SystemModelSerializer
import sift.core.jackson.serializationModule
import sift.core.template.SystemModelTemplate
import sift.core.tree.Tree
import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import kotlin.time.ExperimentalTime

@JsonSerialize(using = SystemModelSerializer.Serializer::class)
@JsonDeserialize(using = SystemModelSerializer.Deserializer::class)
data class SystemModel(
    val entitiesByType: Map<Entity.Type, List<Entity>>,
    val measurements: Tree<Measurement>,
    val statistics: () -> Map<String, Int>
) {
    internal constructor(
        context: Context
    ) : this(
        context.entityService.entitiesByType.map { (type, v) -> type to v.values.toList() }.toMap(),
        context.measurements,
        { context.statistics() }
    )

    internal constructor(
        es: EntityService
    ) : this(es.allEntities().groupBy(Entity::type), Tree(Measurement.NONE), { mapOf() })

    operator fun get(type: Entity.Type): List<Entity> = entitiesByType[type] ?: listOf()
    operator fun contains(type: Entity.Type): Boolean = type in entitiesByType
}

fun saveSystemModel(result: SystemModel, file: File) {
    jacksonObjectMapper()
        .registerModule(serializationModule())
        .writeValueAsString(result)
        .let(file::writeText)
}

fun loadSystemModel(file: File): SystemModel {
    return jacksonObjectMapper()
        .registerModule(serializationModule())
        .readValue(file)
}

@OptIn(ExperimentalTime::class)
fun resolveSystemModel(
    path: String,
    template: SystemModelTemplate?,
    mavenRepostiories: List<URI>,
): SystemModel {
    return if (path.endsWith("json")) {
        // todo: support URI:s
        val f = File(path)
        if (!f.exists()) throw FileNotFoundException(path)

        return loadSystemModel(f)
    } else {
        requireNotNull(template) { "unable to load $path as no template is specified" }

        TemplateProcessor.from(path, mavenRepostiories)
            .process(template.template(), false)
            .let(::SystemModel)
    }
}