package sift.core.api.testdata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.ClassNode
import sift.core.api.*
import sift.core.api.Dsl.classes
import sift.core.asm.classNode
import sift.core.entity.Entity
import sift.core.entity.EntityService

@Disabled
class KnownBugs {

    init {
        debugLog = true
    }

    @Test
    fun `explode Payload in List field and associate property from the main class`(){

        val payload = Entity.Type("payload")

        classes {
            fields {
                signature {
                    typeArguments {
                        explodeType(synthesize = true) {
                            entity(payload)
                        }
                    }
                }
            }
            property(payload, "field-owner", readName())
        }.execute(listOf(classNode(FieldClass::class))) { es ->
            val entities = es[payload].values
            assertThat(entities).hasSize(1)

            entities.first().let { e ->
                assertThat(e["field-owner"]).isEqualTo(listOf("FieldClass"))
            }
        }
    }

    private fun Action<Unit, Unit>.execute(
            cns: List<ClassNode>,
            block: (EntityService) -> Unit
    ) {
        PipelineProcessor(cns)
            .processPipeline(this, false) { it.debugTrails() }
            .entityService
            .also(block)
    }
}

private class FieldClass {
    val payloads: List<PayLoad> = listOf()
}

private class PayLoad