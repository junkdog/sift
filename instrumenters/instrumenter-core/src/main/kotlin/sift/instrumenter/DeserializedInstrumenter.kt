package sift.instrumenter

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.*
import sift.core.api.Action
import sift.core.entity.Entity
import sift.core.jackson.*
import sift.core.terminal.Style

@NoArgConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
internal class DeserializedInstrumenter(
    override val name: String,

    override val defaultType: Entity.Type,
    @field:JsonProperty("entity-types")
    val types: List<Entity.Type>,
    val pipeline: Action<Unit, Unit>,
    val theme: Map<Entity.Type, Style>
) : InstrumenterService {
    override val entityTypes: Iterable<Entity.Type>
        get() = types

    override fun pipeline() = pipeline
    override fun theme() = theme
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
            registerModule(serializationModule())
    }
}

fun InstrumenterService.Companion.deserialize(json: String): InstrumenterService {
    val mapper = mapper()
    val tree = mapper.readTree(json)
    return DeserializedInstrumenter(
        tree["name"].asText(),
        Entity.Type(tree["root"].asText()),
        (tree.get("entity-types") as ArrayNode).map(JsonNode::asText).map(Entity::Type),
        mapper.convertValue<Action.Chain<Unit>>(tree.get("pipeline")),
        mapper.convertValue<HashMap<String, Style>>(tree.get("theme")).map { (k, v) -> Entity.Type(k) to v }.toMap()
    )
}

// save to ~/.local/share/sift/instrumenters/${instrumenter.name}.json
fun InstrumenterService.serialize(): String {
    val mapper = mapper()
    val json = mapper.writeValueAsString(mapper.createObjectNode().apply {
        put("name", name)
        replace("root", mapper.valueToTree(defaultType))
        replace("entity-types", mapper.createArrayNode().apply {
            entityTypes.forEach { add(mapper.valueToTree<JsonNode>(it)) }
        })
        putPOJO("theme", theme())
        replace("pipeline", mapper.valueToTree<ObjectNode>(pipeline()))
    })
    return json
}