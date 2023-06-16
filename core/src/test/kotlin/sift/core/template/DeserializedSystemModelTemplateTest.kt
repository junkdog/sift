package sift.core.template

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import sift.core.api.Action
import sift.core.dsl.template
import sift.core.dsl.type
import sift.core.entity.Entity
import sift.core.terminal.Style
import sift.core.terminal.TextTransformer.Companion.dedupe
import sift.core.terminal.TextTransformer.Companion.idSequence
import sift.core.terminal.TextTransformer.Companion.replace
import sift.core.terminal.TextTransformer.Companion.uuidSequence

class DeserializedSystemModelTemplateTest {
    @Test
    fun `save-load serializer`() {
        val json = SomeTemplate().serialize()
        val deserialized = SystemModelTemplate.deserialize(json)

        assertThat(json).isEqualTo(deserialized.serialize())
    }

    @Test
    fun `save-load label formatters`() {
        val json = LabelFormatterTemplate().serialize()
        val deserialized = SystemModelTemplate.deserialize(json)

        assertThat(json).isEqualTo(deserialized.serialize())
    }
}

private class RestController
private class Endpoint(val path: String, val method: String)
private enum class Yolo { Foo, Bar }

class LabelFormatterTemplate : SystemModelTemplate {
    val e = Entity.Type("foobar")
    override val name: String = "test"
    override val description: String = ""
    override val defaultType: Entity.Type = Entity.Type("controller")
    override val entityTypes: Iterable<Entity.Type> = listOf(e)
    override fun template() = template {
        classes {
            entity(e, label("hi",
                replace("abc", "def"),
                uuidSequence(),
                dedupe(' '),
                replace("abc", "def"),
                replace("abc", "def"),
            ))
            property(e, "dot-label-transform", editor(
                replace("abc", "def"),
                dedupe(' '),
                uuidSequence(),
                idSequence(Regex("abc")),
            ))
        }
    }

    override fun theme(): Map<Entity.Type, Style> = mapOf()
}

class SomeTemplate : SystemModelTemplate {
    override val name: String = "test"
    override val description: String = ""
    override val defaultType: Entity.Type = Entity.Type("controller")
    override val entityTypes: Iterable<Entity.Type> = listOf(
        Entity.Type("controller"),
    )

    override fun template() = template {
        include(SynthesisPipeline().template())

        classes {
            annotatedBy<RestController>()
            entity(Entity.Type("controller"),
                property("int", withValue(1)),
                property("yolo", withValue(Yolo.Foo)))
        }
    }

    override fun theme(): Map<Entity.Type, Style> = mapOf()
}

class SynthesisPipeline : SystemModelTemplate {
    val e = Entity.Type("synth")

    override val name: String = "test-synth"
    override val description: String = ""
    override val defaultType: Entity.Type = e
    override val entityTypes: Iterable<Entity.Type> = listOf(e)
    override fun template(): Action<Unit, Unit> {
        return template {
            synthesize {
                entity(e, type("java.lang.String"), label("\${a} \${b} \${c:yo}"))
            }

            classesOf(e) {
                logCount("foo")
                filter(e)
                filter("String", invert = true)
                scope("scoping") {
                    property(e, "a", readName())
                    property(e, "b", readType())
                    property(e, "c", withValue(true))
                }
            }
        }
    }

    override fun theme(): Map<Entity.Type, Style> = mapOf()
}