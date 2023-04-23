package sift.core.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import sift.core.dsl.Type

object SiftTypeSerializer {
    class Serializer : JsonSerializer<Type>() {
        override fun serialize(
            value: Type,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) = when {
            value.isPrimitive -> gen.writeString("!${value.value}")
            else              -> gen.writeString(value.value)
        }
    }

    class Deserializer : JsonDeserializer<Type>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): Type = p.readValueAs<String>().let { raw ->
            val isPrimitive = (raw.startsWith("!"))
            when {
                isPrimitive -> Type.primitiveType(raw[1])
                else        -> Type.from(raw)
            }
        }
    }
}

