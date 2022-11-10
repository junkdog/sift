package sift.core.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import sift.core.api.Measurement
import sift.core.api.MeasurementScope
import sift.core.api.PipelineResult
import sift.core.entity.Entity
import sift.core.tree.Tree
import java.util.*
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

object PipelineResultSerializer {
    class Serializer : JsonSerializer<PipelineResult>() {
        override fun serialize(
            value: PipelineResult,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) {
            gen.writeStartArray()
            value.entitiesByType.values
                .flatten()
                .forEach { e -> gen.writeEntity(e, serializers) }
            gen.writeEndArray()
        }


        private fun JsonGenerator.writeEntity(e: Entity, serializers: SerializerProvider) {
            writeStartObject()

            writeStringField("id", e.id.toString())
            writeStringField("type", e.type.id)
            writeStringField("label", e.label)
            writeEntityChildren(e)
            writeEntityProperties(e, serializers)

            writeEndObject()
        }

        private fun JsonGenerator.writeEntityChildren(e: Entity) {
            writeObjectFieldStart("children")
            e.children.forEach { (key, entities) ->
                writeArrayFieldStart(key)
                entities.map(Entity::id).map(UUID::toString).forEach(::writeString)
                writeEndArray()
            }
            writeEndObject()
        }

        private fun JsonGenerator.writeEntityProperties(
            e: Entity,
            serializers: SerializerProvider,
        ) {
            writeObjectFieldStart("properties")

            e.properties.forEach { (property, values) ->
                writeArrayFieldStart(property)

                // json fields: @type and value
                values.forEach { v ->
                    writeStartObject()
                    writeStringField("type", v::class.qualifiedName)

                    writeFieldName("value")
                    serializers.findValueSerializer(v::class.java)
                        .serialize(v, this, serializers)

                    writeEndObject() // property key-values
                }

                writeEndArray()
            }

            writeEndObject() // properties
        }
    }

    class Deserializer : JsonDeserializer<PipelineResult>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): PipelineResult {
            require(p.isExpectedStartArrayToken)

            val deserialized = generateSequence(p::nextToken)
                .mapNotNull { _ -> jsonEntity(p, ctxt) }
                .toList()

            val entities = deserialized
                .map { e -> Entity(e.type, e.label, mapOf(), e.id) }
                .associateBy(Entity::id)

            deserialized.forEach { e ->
                // associate child entities
                e.children.forEach { (key, children) ->
                    entities[e.id]!! .addChildren(key, children.map { entities[it]!! })
                }

                // transfer properties
                entities[e.id]!!.properties
                    .putAll(e.properties.mapValues { (_, v) -> v.filterNotNull().toMutableList() })
            }

            return PipelineResult(
                entitiesByType = entities.values.groupBy(Entity::type),
                measurements = Tree(Measurement(".", MeasurementScope.Instrumenter, MeasurementScope.Instrumenter, 0, 0, 0.milliseconds))
            )
        }

        private fun jsonEntity(
            p: JsonParser,
            ctxt: DeserializationContext
        ): JsonEntity? {
            if (!p.isExpectedStartObjectToken)
                return null

            // id
            p.nextValue()
            require(p.currentName == "id")
            val id = UUID.fromString(p.readValueAs(String::class.java))
            // type
            p.nextValue()
            require(p.currentName == "type")
            val type = Entity.Type(p.readValueAs(String::class.java))
            // label
            p.nextValue()
            require(p.currentName == "label")
            val label = p.readValueAs(String::class.java)
            // children
            p.nextValue()
            require(p.currentName == "children")
            val children = p.codec.readTree<ObjectNode>(p)
                .fields()
                .asSequence()
                .map { (key, entities) -> key to entities.map(JsonNode::asText).map(UUID::fromString) }
                .toMap()
                .toMutableMap()

            // properties
            p.nextValue()
            require(p.currentName == "properties")
            val properties = p.codec.readTree<ObjectNode>(p)
                .fields()
                .asSequence()
                .map { (k, v) -> k to propertyValues(v, ctxt) }
                .toMap()
                .toMutableMap()

            return JsonEntity(id, type, label, children, properties)
        }

        private fun propertyValues(
            node: JsonNode,
            ctxt: DeserializationContext
        ): List<*> {
            return (node as ArrayNode)
                .map { it as ObjectNode }
                .map { it["type"].asText() to it["value"] }
                .map { (type, node) -> when(type) {
                    "sift.core.entity.Entity.Type" -> Entity.Type(node.asText())
                    "kotlin.String"                -> node.asText()
                    else                           -> ctxt.readTreeAsValue(node, Class.forName(type))
                } }
        }
    }
}

data class JsonEntity(
    val id: UUID,
    val type: Entity.Type,
    val label: String,
    val children: MutableMap<String, List<UUID>>,
    val properties: MutableMap<String, List<*>>
)

data class TypedValue<T : Any>(val type: KClass<T>, val value: T)