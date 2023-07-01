package sift.template.springboot

import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import sift.core.entity.Entity
import sift.core.api.Action
import sift.core.dsl.*
import sift.core.dsl.ScopeEntityPredicate.ifExists
import sift.core.graphviz.Dot
import sift.core.terminal.Gruvbox.light2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.dedupe
import sift.core.terminal.TextTransformer.Companion.edit
import sift.core.terminal.TextTransformer.Companion.replace
import sift.core.terminal.TextTransformer.Companion.uppercase
import sift.template.dsl.graphviz
import sift.template.spi.SystemModelTemplateServiceProvider

typealias A = SpringBootTemplate.Annotation
typealias E = SpringBootTemplate.EntityType

@Suppress("unused")
class SpringBootTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {

    object Annotation {
        // spring
        val deleteMapping = "org.springframework.web.bind.annotation.DeleteMapping".type
        val getMapping = "org.springframework.web.bind.annotation.GetMapping".type
        val patchMapping = "org.springframework.web.bind.annotation.PatchMapping".type
        val postMapping = "org.springframework.web.bind.annotation.PostMapping".type
        val putMapping = "org.springframework.web.bind.annotation.PutMapping".type
        val httpMapping = Regex("org.springframework.web.bind.annotation\\.[A-Z][^.]*Mapping").type
        val requestBody = "org.springframework.web.bind.annotation.RequestBody".type
        val requestMapping = "org.springframework.web.bind.annotation.RequestMapping".type

        val restControllers = Regex("org.springframework.(stereotype.Controller|web.bind.annotation.RestController)").type
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
            scope("register controllers") {
                annotatedBy(A.restControllers)
                entity(E.controller)

                methods {
                    scope("register endpoints") {
                        annotatedBy(A.httpMapping) // matches any @.*Mapping
                        entity(E.endpoint, label("\${http-method} /\${base-path:}/\${path:}", dedupe('/')),
                            property("path", readAnnotation(A.httpMapping, "value")),
                        )

                        annotations("extract http method name from annotation", A.httpMapping) {
                            // @PostMapping -> POST, @DeleteMapping -> DELETE, etc
                            property(E.endpoint, "http-method",
                                readName() andThen replace("Mapping", "") andThen uppercase())
                        }

                        outerScope("read base path from @RequestMapping") {
                            property(E.endpoint, "base-path", readAnnotation(A.requestMapping, "value"))
                        }

                        parameters {
                            annotatedBy(A.requestBody)
                            property(E.endpoint, "request-object", readType())
                        }
                    }

                    // not all controllers are REST controllers (@MessageMapping/rsocket endpoints)
                    scope("attempt registering endpoints", ifExists, E.endpoint) {
                       E.controller["endpoints"] = E.endpoint
                    }
                }
            }
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
