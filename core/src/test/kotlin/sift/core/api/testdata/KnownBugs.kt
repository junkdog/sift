package sift.core.api.testdata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.ClassNode
import sift.core.api.*
import sift.core.api.Dsl.classes
import sift.core.api.testdata.set1.Payload
import sift.core.api.testdata.set2.HandlerFn
import sift.core.api.testdata.set2.HandlerOfFns
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

    @Test
    fun `correctly identify relations when scanning instantiations`() {
        val cns = listOf(
            classNode<HandlerFn>(),
            classNode<Payload>(),
            classNode<HandlerOfFns>(),
        )

        val handler = Entity.Type("handler")
        val data = Entity.Type("data")

        classes {
            scope("scan handler") {
                methods {
                    annotatedBy<HandlerFn>()
                    entity(handler)

                    parameters {
                        parameter(0)
                        explodeType {
                            entity(data)
                        }
                    }

//                    data["sent-by"] = handler

                    // works; reverse lookup via "backtrack" children
//                    handler["sent-by"] = data.instantiations

                    instantiationsOf(data) {
                        log("data")
                        data["sent-by"] = handler
                    }
                }
            }
        }.execute(cns) { es ->
            assertThat(es[data].values.first().children["sent-by"]!!.map(Entity::toString))
                .containsExactlyInAnyOrder(
                    e(handler, "HandlerOfFns::boo").toString(),
                )
        }
    }

    private fun Action<Unit, Unit>.execute(
            cns: List<ClassNode>,
            block: (EntityService) -> Unit
    ) {
        PipelineProcessor(cns)
            .processPipeline(this, false)
            .entityService
            .also(block)
    }
}

private class FieldClass {
    val payloads: List<PayLoad> = listOf()
}

private class PayLoad