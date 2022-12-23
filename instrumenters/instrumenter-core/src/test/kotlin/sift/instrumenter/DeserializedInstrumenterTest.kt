package sift.instrumenter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import sift.core.api.Dsl.instrumenter
import sift.core.entity.Entity
import sift.core.terminal.Style

class DeserializedInstrumenterTest {
    @Test
    fun `save-load serializer`() {
        val json = SomeInstrumenterPipeline().serialize()
        val deserialized = InstrumenterService.deserialize(json)

        assertThat(json).isEqualTo(deserialized.serialize())
    }
}

private class RestController
private class Endpoint(val path: String, val method: String)
private enum class Yolo { Foo, Bar }

class SomeInstrumenterPipeline : InstrumenterService {
    override val name: String = "test"
    override val defaultType: Entity.Type = Entity.Type("controller")
    override val entityTypes: Iterable<Entity.Type> = listOf(
        Entity.Type("controller"),
    )

    override fun pipeline() = instrumenter {
        classes {
            annotatedBy<RestController>()
            entity(Entity.Type("controller"),
                property("int", withValue(1)),
                property("yolo", withValue(Yolo.Foo)))
        }
    }

    override fun theme(): Map<Entity.Type, Style> = mapOf()
    override fun create() = this

}