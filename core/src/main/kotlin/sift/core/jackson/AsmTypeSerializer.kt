package sift.core.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.objectweb.asm.Type

object AsmTypeSerializer {
    class Serializer : JsonSerializer<Type>() {
        override fun serialize(value: Type, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(value.className)
        }
    }

    class Deserializer : JsonDeserializer<Type>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): Type {
            val cls = p.readValueAs<String>()
            return Type.getType("L${cls.replace('.', '/')};")
        }
    }
}

