package sift.core.api

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import sift.core.entity.Entity
import sift.core.jackson.SystemModelSerializer
import sift.core.jackson.serializationModule
import sift.core.tree.Tree
import java.io.File

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