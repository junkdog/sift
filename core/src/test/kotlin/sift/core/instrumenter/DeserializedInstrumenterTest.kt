package sift.core.instrumenter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import sift.core.api.Action
import sift.core.api.Dsl.instrumenter
import sift.core.asm.type
import sift.core.entity.Entity
import sift.core.terminal.Style
import sift.core.terminal.TextTransformer
import sift.core.terminal.TextTransformer.Companion.dedupe
import sift.core.terminal.TextTransformer.Companion.replace
import sift.core.terminal.TextTransformer.Companion.uuidSequence

class DeserializedInstrumenterTest {
    @Test
    fun `save-load serializer`() {
        val json = SomeInstrumenterPipeline().serialize()
        val deserialized = InstrumenterService.deserialize(json)

        assertThat(json).isEqualTo(deserialized.serialize())
    }

    @Test
    fun `save-load label formatters`() {
        val json = LabelFormatterPipeline().serialize()
        val deserialized = InstrumenterService.deserialize(json)

        assertThat(json).isEqualTo(deserialized.serialize())
    }
}

private class RestController
private class Endpoint(val path: String, val method: String)
private enum class Yolo { Foo, Bar }

class LabelFormatterPipeline : InstrumenterService {
    val e = Entity.Type("foobar")
    override val name: String = "test"
    override val defaultType: Entity.Type = Entity.Type("controller")
    override val entityTypes: Iterable<Entity.Type> = listOf(e)
    override fun pipeline() = instrumenter {
        classes {
            entity(e, label("hi",
                replace("abc", "def"),
                uuidSequence(),
                dedupe(' '),
                replace("abc", "def"),
                replace("abc", "def"),
            ))
        }
    }

    override fun theme(): Map<Entity.Type, Style> = mapOf()

}

class SomeInstrumenterPipeline : InstrumenterService {
    override val name: String = "test"
    override val defaultType: Entity.Type = Entity.Type("controller")
    override val entityTypes: Iterable<Entity.Type> = listOf(
        Entity.Type("controller"),
    )

    override fun pipeline() = instrumenter {
        include(SynthesisPipeline().pipeline())

        classes {
            annotatedBy<RestController>()
            entity(Entity.Type("controller"),
                property("int", withValue(1)),
                property("yolo", withValue(Yolo.Foo)))
        }
    }

    override fun theme(): Map<Entity.Type, Style> = mapOf()
}

class SynthesisPipeline : InstrumenterService {
    val e = Entity.Type("synth")

    override val name: String = "test-synth"
    override val defaultType: Entity.Type = e
    override val entityTypes: Iterable<Entity.Type> = listOf(e)
    override fun pipeline(): Action<Unit, Unit> {
        return instrumenter {
            synthesize {
                entity(e, type<String>(), label("\${a} \${b} \${c:yo}"))
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