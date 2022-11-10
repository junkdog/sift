package sift.core.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

object RegexSerializer {
    class Serializer : JsonSerializer<Regex>() {
        override fun serialize(value: Regex, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(value.pattern)
        }
    }

    class Deserializer : JsonDeserializer<Regex>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): Regex {
            val pattern = p.readValueAs(String::class.java)
            return Regex(pattern)
        }
    }
}