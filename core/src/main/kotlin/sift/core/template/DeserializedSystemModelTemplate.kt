package sift.core.template

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
import java.io.File
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

@NoArgConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
internal class DeserializedSystemModelTemplate(
    override val name: String,
    override val description: String = "Deserialized $name template.",

    override val defaultType: Entity.Type,
    @field:JsonProperty("entity-types")
    val types: List<Entity.Type>,
    val pipeline: Action<Unit, Unit>,
    val theme: Map<Entity.Type, Style>
) : SystemModelTemplate {
    override val entityTypes: Iterable<Entity.Type>
        get() = types

    override fun template() = pipeline
    override fun theme() = theme

    companion object {
        internal var deserializationTime: kotlin.time.Duration = 0.milliseconds
    }
}


private fun mapper() : ObjectMapper {
    return jacksonObjectMapper().apply {
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

fun SystemModelTemplate.Companion.deserialize(json: String): SystemModelTemplate {

    val (template, duration) = measureTimedValue {
        val mapper = mapper()
        val tree = mapper.readTree(json)

        DeserializedSystemModelTemplate(
            tree["name"].asText(),
            tree["description"]?.asText() ?: "Deserialized ${tree["name"].asText()} template.",
            Entity.Type(tree["root"].asText()),
            (tree.get("entity-types") as ArrayNode).map(JsonNode::asText).map(Entity::Type),
            mapper.convertValue<Action.Chain<Unit>>(tree.get("pipeline")),
            mapper.convertValue<HashMap<String, Style>>(tree.get("theme")).map { (k, v) -> Entity.Type(k) to v }.toMap()
        )
    }

    DeserializedSystemModelTemplate.deserializationTime = duration

    return template
}

// save to ~/.local/share/sift/templates/${template.name}.json
fun SystemModelTemplate.serialize(): String {
    val mapper = mapper()
    val json = mapper.writeValueAsString(mapper.createObjectNode().apply {
        put("name", name)
        put("description", description)
        replace("root", mapper.valueToTree(defaultType))
        replace("entity-types", mapper.createArrayNode().apply {
            entityTypes.forEach { add(mapper.valueToTree<JsonNode>(it)) }
        })
        putPOJO("theme", theme())
        replace("pipeline", mapper.valueToTree<ObjectNode>(template()))
    })
    return json
}

fun SystemModelTemplate.save() {
    resolveTemplateFile(name).writeText(serialize())
}

fun SystemModelTemplate.Companion.load(name: String): SystemModelTemplate {
    return resolveTemplateFile(name)
        .takeIf(File::canRead)
        ?.let(File::readText)
        ?.let(SystemModelTemplate.Companion::deserialize)
        ?: error("Unable to read template with name $")
}

private fun resolveTemplateFile(templateName: String): File {
    return File("${System.getProperty("user.home")}/.local/share/sift/templates")
        .also(File::mkdirs)
        .resolve("${templateName}.json")
}