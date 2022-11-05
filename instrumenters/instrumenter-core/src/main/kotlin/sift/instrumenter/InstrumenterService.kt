package sift.instrumenter

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import org.objectweb.asm.Type
import sift.core.api.Action
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.jackson.AsmTypeSerializer
import sift.core.jackson.RegexSerializer
import sift.core.jackson.WithValueSerializer
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.instrumenter.spi.InstrumenterServiceProvider

interface InstrumenterService : InstrumenterServiceProvider {
    val name: String
    val entityTypes: Iterable<Entity.Type>

    fun pipeline(): Action<Unit, Unit>
    fun toTree(es: EntityService, forType: Entity.Type?): Tree<EntityNode>
    fun theme(): Map<Entity.Type, Style>
}

private fun mapper() : ObjectMapper {
    return ObjectMapper().apply {
            registerModule(
                KotlinModule.Builder()
                    .withReflectionCacheSize(512)
                    .configure(KotlinFeature.NullToEmptyCollection, false)
                    .configure(KotlinFeature.NullToEmptyMap, false)
                    .configure(KotlinFeature.NullIsSameAsDefault, false)
                    .configure(KotlinFeature.SingletonSupport, true)
                    .configure(KotlinFeature.StrictNullChecks, false)
                    .build()
            )
            activateDefaultTyping(polymorphicTypeValidator, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE)
            setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
            setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            registerModule(SimpleModule()
                .addSerializer(Type::class, AsmTypeSerializer.Serializer())
                .addDeserializer(Type::class, AsmTypeSerializer.Deserializer())
                .addSerializer(Regex::class, RegexSerializer.Serializer())
                .addDeserializer(Regex::class, RegexSerializer.Deserializer())
                .addSerializer(Action.WithValue::class, WithValueSerializer.Serializer())
                .addDeserializer(Action.WithValue::class, WithValueSerializer.Deserializer())
            )
        }
}

fun InstrumenterService.serialize(): String {
    val mapper = mapper()
    val json = mapper.writeValueAsString(mapOf(
        "name" to name,
        "entity-types" to mapper.valueToTree(entityTypes.toList()),
        "pipeline" to mapper.valueToTree<ObjectNode>(pipeline()),
        "theme" to theme()
    ))
    return json
}