package sift.template.springboot

import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Action
import sift.core.dsl.Methods
import sift.core.dsl.ScopeEntityPredicate
import sift.core.dsl.template
import sift.core.graphviz.Dot
import sift.core.terminal.Gruvbox.light2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.dedupe
import sift.template.dsl.graphviz
import sift.template.spi.SystemModelTemplateServiceProvider

typealias A = SpringBootTemplate.Annotation
typealias E = SpringBootTemplate.EntityType

@Suppress("unused")
class SpringBootTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {

    object Annotation {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

        // spring
        val controller = "org.springframework.stereotype.Controller".type
        val deleteMapping = "org.springframework.web.bind.annotation.DeleteMapping".type
        val getMapping = "org.springframework.web.bind.annotation.GetMapping".type
        val patchMapping = "org.springframework.web.bind.annotation.PatchMapping".type
        val postMapping = "org.springframework.web.bind.annotation.PostMapping".type
        val putMapping = "org.springframework.web.bind.annotation.PutMapping".type
        val requestBody = "org.springframework.web.bind.annotation.RequestBody".type
        val requestMapping = "org.springframework.web.bind.annotation.RequestMapping".type
        val restController = "org.springframework.web.bind.annotation.RestController".type
    }

    object EntityType {
        internal val types: MutableList<Entity.Type> = mutableListOf()

        private val String.type
            get() = Entity.Type(this).also { types += it }

        // spring
        val controller = "controller".type
        val endpoint = "endpoint".type
    }

    override val defaultType: Entity.Type = E.controller
    override val entityTypes: Iterable<Entity.Type> = E.types

    override fun create() = this
    override val name: String
        get() = "spring-boot"

    override val description: String = """
        |Spring Boot supporting template registering controllers and endpoints.
    """.trimMargin()

    override fun template(): Action<Unit, Unit> = template {
        classes {
            fun registerController(controller: Type) {
                scope("register controllers") {
                    annotatedBy(controller)
                    entity(E.controller)

                    methods {
                        fun Methods.registerEndpoints(method: String, httpMethod: Type) {
                            scope(method) {
                                annotatedBy(httpMethod)
                                entity(E.endpoint,
                                    label("\${http-method} /\${base-path:}\${path:}", dedupe('/')),
                                    property("http-method", withValue(method)),
                                    property("path", readAnnotation(httpMethod, "value")),
                                )

                                outerScope("read base path from @RequestMapping") {
                                    property(E.endpoint, "base-path", readAnnotation(A.requestMapping, "value"))
                                }

                                parameters {
                                    annotatedBy(A.requestBody)
                                    property(E.endpoint, "request-object", readType())
                                }
                            }
                        }

                        // maps to EntityType.endpoint
                        registerEndpoints("DELETE", A.deleteMapping)
                        registerEndpoints("GET", A.getMapping)
                        registerEndpoints("PATCH", A.patchMapping)
                        registerEndpoints("POST", A.postMapping)
                        registerEndpoints("PUT", A.putMapping)

                        // not all controllers are REST controllers (@MessageMapping/rsocket endpoints)
                        scope("attempt registering endpoints", ScopeEntityPredicate.ifExists, E.endpoint) {
                           E.controller["endpoints"] = E.endpoint
                        }
                    }
                }
            }

            registerController(A.controller)
            registerController(A.restController)
        }

        scope("dot graph property configuration") {
            graphviz(E.endpoint, rank = 0, type = Dot.node)
        }
    }

    override fun theme() = mapOf(
        E.controller to plain(light2 + inverse),
        E.endpoint   to plain(light2 + bold),
    )
}
