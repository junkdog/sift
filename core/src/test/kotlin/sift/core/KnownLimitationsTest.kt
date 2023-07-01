package sift.core

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.ClassNode
import sift.core.api.*
import sift.core.api.testdata.set1.Payload
import sift.core.api.testdata.set2.HandlerFn
import sift.core.api.testdata.set2.HandlerOfFns
import sift.core.asm.classNode
import sift.core.dsl.annotatedBy
import sift.core.dsl.classes
import sift.core.entity.Entity
import sift.core.entity.EntityService

@Disabled
class KnownLimitationsTest {

    init {
        debugLog = true
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

//                     works; reverse lookup via "backtrack" children
//                    data.instantiations["sent-by"] = handler

                    instantiationsOf(data) {
                        data["sent-by"] = handler
                    }
                }
            }
        }.execute(cns) { es ->
//            assertThat(es[data].values.first().children["sent-by"]!!.map(Entity::toString))
            Assertions.assertThat(es[data].values.first().children["sent-by"]!!.map(Entity::toString))
                .containsExactlyInAnyOrder(
                    "Entity(HandlerOfFns::on, type=handler)",
                    "Entity(HandlerOfFns::boo, type=handler)",
                )
        }
    }

    private fun Action<Unit, Unit>.execute(
        cns: List<ClassNode>,
        block: (EntityService) -> Unit
    ) {
        TemplateProcessor(cns)
            .process(this, false, Context::debugTrails)
            .entityService
            .also(block)
    }
}