package sift.core.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import sift.core.dsl.Type
import sift.core.dsl.type

object SiftTypeSerializer {
    class Serializer : JsonSerializer<Type>() {
        override fun serialize(
            value: Type,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) = gen.writeString(value.toString())
    }

    class Deserializer : JsonDeserializer<Type>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): Type = p.readValueAs<String>().type
    }
}

