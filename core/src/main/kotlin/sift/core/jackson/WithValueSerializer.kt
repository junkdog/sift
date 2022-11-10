package sift.core.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.type.WritableTypeId
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.node.ObjectNode
import sift.core.api.Action
import sift.core.api.Element
import sift.core.entity.Entity

object WithValueSerializer {
    class Serializer : JsonSerializer<Action.WithValue<*>>() {
        override fun serializeWithType(
            value: Action.WithValue<*>,
            gen: JsonGenerator,
            serializers: SerializerProvider,
            typeSer: TypeSerializer
        ) {
            val typeId: WritableTypeId = typeSer.typeId(value, JsonToken.START_OBJECT)
            typeSer.writeTypePrefix(gen, typeId)
            serialize(value, gen, serializers)
            typeSer.writeTypeSuffix(gen, typeId)
        }

        override fun serialize(value: Action.WithValue<*>, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeStringField("@type", value.value::class.qualifiedName)
            when (val v = value.value) {
                is String  -> gen.writeStringField("value", v)
                is Boolean -> gen.writeBooleanField("value", v)
                is Long    -> gen.writeNumberField("value", v)
                is Int     -> gen.writeNumberField("value", v)
                is Short   -> gen.writeNumberField("value", v)
                is Float   -> gen.writeNumberField("value", v)
                is Double  -> gen.writeNumberField("value", v)
                else       -> gen.writeObjectField("value", v)
            }
        }
    }

    class Deserializer : JsonDeserializer<Action.WithValue<*>>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): Action.WithValue<*> {
            val node = p.codec.readTree<ObjectNode>(p)
            val v = when (val cls = node["@type"].asText()) {
                "sift.core.entity.Entity.Type" -> Entity.Type(node["value"].asText())
                "kotlin.String"                -> node["value"].asText()
                else -> ctxt.readValue(node["value"].traverse(), Class.forName(cls))
            }

            return Action.WithValue<Element.Value>(v) as Action.WithValue<*>
        }
    }
}