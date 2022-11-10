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
import sift.core.entity.Entity

object EntityTypeSerializer {
    class Serializer : JsonSerializer<Entity.Type>() {
        override fun serialize(
            value: Entity.Type,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) {
            gen.writeString(value.id)
        }

        override fun serializeWithType(
            value: Entity.Type,
            gen: JsonGenerator,
            serializers: SerializerProvider,
            typeSer: TypeSerializer
        ) {
            val typeId: WritableTypeId = typeSer.typeId(value, JsonToken.START_OBJECT)
            typeSer.writeTypePrefix(gen, typeId)
            gen.writeStringField("id", value.id)
            typeSer.writeTypeSuffix(gen, typeId)
        }
    }

    class Deserializer : JsonDeserializer<Entity.Type>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): Entity.Type {
            return Entity.Type(p.readValueAs(String::class.java))
        }
    }
}