package sift.template.jakartaws

import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import sift.core.dsl.*
import sift.core.entity.Entity
import sift.core.graphviz.Dot
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Gruvbox.light2
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.dedupe
import sift.template.dsl.graphviz
import sift.template.spi.SystemModelTemplateServiceProvider

typealias E = JakartaWsTemplate.EntityTypes
typealias A = JakartaWsTemplate.Annotations

@Suppress("unused")
class JakartaWsTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {

    override val entityTypes: Iterable<Entity.Type> = listOf(E.controller, E.endpoint)
    override val defaultType: Entity.Type = entityTypes.first()

    object Annotations {
        val httpDelete = Regex("^(javax|jakarta).ws.rs.DELETE").type
        val httpGet    = Regex("^(javax|jakarta).ws.rs.GET").type
        val httpPost   = Regex("^(javax|jakarta).ws.rs.POST").type
        val httpPut    = Regex("^(javax|jakarta).ws.rs.PUT").type

        val produces   = Regex("^(javax|jakarta).ws.rs.Produces").type
        val consumes   = Regex("^(javax|jakarta).ws.rs.Consumes").type

        val path = Regex("^(javax|jakarta).ws.rs.Path").type

        val applicationPath = Regex("^(javax|jakarta).ws.rs.ApplicationPath").type
    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val controller = "controller".type
        val endpoint = "endpoint".type
    }

    override fun create() = this
    override val name: String
        get() = "jakarta-ws"

    override val description: String = """
        |Supporting template for Jakarta RESTful Web Services.
    """.trimMargin()

    private fun Methods.registerEndpoints(httpMethod: SiftType) {
        scope("registering HTTP ${httpMethod.simpleName} endpoints") {
            annotatedBy(httpMethod)
            entity(E.endpoint,
                label("\${http-method} /\${base-path:}/\${path:}", dedupe('/')),
                property("path", readAnnotation(A.path, "value")),
                property("http-method", withValue(httpMethod.simpleName))
            )

            outerScope("associate ${httpMethod.simpleName} endpoints with controller") {
                entity(E.controller)
                E.controller["endpoints"] = E.endpoint

                // reading class-level @Path from hosting controller class
                property(E.endpoint, "base-path", readAnnotation(A.path, "value"))
            }
        }
    }

    override fun template() = template {
        classes {
            methods {
                registerEndpoints(A.httpDelete)
                registerEndpoints(A.httpGet)
                registerEndpoints(A.httpPost)
                registerEndpoints(A.httpPut)
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