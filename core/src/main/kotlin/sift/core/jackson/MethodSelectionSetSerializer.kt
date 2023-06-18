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
import sift.core.dsl.MethodSelectionSet
import sift.core.dsl.MethodSelection

object MethodSelectionSetSerializer {
    internal class Serializer : JsonSerializer<MethodSelectionSet>() {
        override fun serialize(
            value: MethodSelectionSet,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) {
            gen.writeString(value.toString())
        }

        override fun serializeWithType(
            value: MethodSelectionSet,
            gen: JsonGenerator,
            serializers: SerializerProvider,
            typeSer: TypeSerializer
        ) {
            val typeId: WritableTypeId = typeSer.typeId(value, JsonToken.START_OBJECT)
            typeSer.writeTypePrefix(gen, typeId)
            gen.writeStringField("selection", value.toString())
            typeSer.writeTypeSuffix(gen, typeId)
        }
    }

    internal class Deserializer : JsonDeserializer<MethodSelectionSet>() {
        private val values = enumValues<MethodSelection>()
            .associateBy(MethodSelection::name)

        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): MethodSelectionSet {
            return p.readValueAs<String>()
                .split(" + ")
                .map(values::getValue)
                .toSet()
                .let(::MethodSelectionSet)
        }
    }
}