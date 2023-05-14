package sift.core.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import sift.core.dsl.RegexType
import sift.core.dsl.SiftType
import sift.core.dsl.Type
import sift.core.dsl.type

object SiftTypeSerializer {
    class Serializer : JsonSerializer<SiftType>() {
        override fun serialize(
            value: SiftType,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) = when (value) {
            is RegexType -> gen.writeString("r#${value.match.pattern}")
            is Type -> when {
                value.isPrimitive -> gen.writeString("!${value.value}")
                else              -> gen.writeString(value.value)
            }
        }
    }

    class Deserializer : JsonDeserializer<SiftType>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): SiftType = p.readValueAs<String>().let { raw ->
            when {
                raw.startsWith("!")  -> Type.primitiveType(raw[1])
                raw.startsWith("r#") -> Regex(raw.substring(2)).type
                else                 -> Type.from(raw)
            }
        }
    }
}
