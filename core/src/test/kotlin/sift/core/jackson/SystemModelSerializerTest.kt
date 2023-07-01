package sift.core.jackson

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import sift.core.api.AccessFlags.acc_final
import sift.core.api.Context
import sift.core.api.SystemModel
import sift.core.api.TemplateProcessor
import sift.core.api.debugLog
import sift.core.api.debugTrails
import sift.core.asm.classNode
import sift.core.dsl.annotatedBy
import sift.core.dsl.classes
import sift.core.entity.Entity

class SystemModelSerializerTest {

    @Test
    fun `serialization happy path`() {
        debugLog = true

        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        val cns = listOf(
            classNode(ControllerA::class),
            classNode(ControllerB::class),
            classNode(Endpoint::class),
        )

        val template = classes {
            filter(acc_final)
            log("classes")
            entity(controller)
            methods {
                log("methods")
                annotatedBy<Endpoint>()
                entity(endpoint)

                outerScope("register controller class") {
                    log("iterating set of classes with @Endpoint methods")
                    controller["endpoints"] = endpoint
                }
            }
        }

        val sm = TemplateProcessor(cns)
            .process(template, false, Context::debugTrails)
            .entityService
            .let(::SystemModel)

        val mapper = jacksonObjectMapper()
            .registerModule(serializationModule())
        assertThat(sm.entitiesByType).isNotEmpty
        assertThat(sm.entitiesByType).isEqualTo(
            mapper.writeValueAsString(sm).let { mapper.readValue<SystemModel>(it).entitiesByType }
        )
    }
}

private annotation class Endpoint

private class ControllerA {
    @Endpoint
    fun a() = Unit
}

private class ControllerB {
    @Endpoint
    fun b1() = Unit
    @Endpoint
    fun bb() = Unit
}