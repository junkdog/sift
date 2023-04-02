package sift.template.sandbox

import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import sift.core.dsl.Classes
import sift.core.dsl.type
import sift.core.dsl.template
import sift.core.entity.Entity
import sift.core.graphviz.Dot
import sift.core.graphviz.Shape
import sift.core.template.SystemModelTemplate
import sift.core.template.save
import sift.core.terminal.Gruvbox.light2
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.replace
import sift.template.dsl.graphviz
import sift.template.sbacqrs.SpringBootAxonCqrsTemplate

typealias E = SandboxTemplate.EntityTypes
typealias A = SandboxTemplate.Annotations
typealias T = SandboxTemplate.AsmTypes

typealias SBAE = SpringBootAxonCqrsTemplate.EntityType

@Suppress("unused")
class SandboxTemplate : SystemModelTemplate {
    val springAxon = SpringBootAxonCqrsTemplate()

    override val entityTypes: Iterable<Entity.Type> = listOf(E.client, E.scheduled) + springAxon.entityTypes
    override val defaultType: Entity.Type = E.client

    object Annotations {
        val scheduled = "org.springframework.scheduling.annotation.Scheduled".type
        val component = "org.springframework.stereotype.Component".type
    }

    object AsmTypes {
        val dynamoDbClient = "software.amazon.awssdk.services.dynamodb.DynamoDbClient".type
    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val client = "client".type
        val scheduled = "scheduled".type

        val dynamoRepository = "dynamo-repository".type
        val dynamoMethod = "dynamo".type
    }

    override val name: String
        get() = "dynamo-example"

    override val description: String = "for dynamodb extension for axon example"


    override fun template() = template {
        // sets up everything except @Scheduled
        include(springAxon.template())

        classes {
            methods {
                annotatedBy(A.scheduled)
                entity(E.scheduled, label("\uD83D\uDD03 every:\${rate}s \${name}", replace("000s", "s")),
                    property("rate", readAnnotation(A.scheduled, "fixedDelay")),
                    property("name", readName()),
                )

                outerScope("register client class and associate @Scheduled entities") {
                    entity(E.client)
                    E.client["schedules"] = E.scheduled
                }
            }
        }

        scope("dynamo repositories") {
            classes {
                annotatedBy(A.component)
                filter(Regex("Repository$"))
                fields {
                    fun Classes.registerDynamoDb() {
                        entity(E.dynamoRepository)
                        methods {
                            filter(Regex("^(get|<init>)"), invert = true)
                            filter(Regex("^string.*Value"), invert = true)
                            filter("\\\$lambda", invert = true)

                            entity(E.dynamoMethod)
                            E.dynamoRepository["methods"] = E.dynamoMethod
                        }
                    }

                    filterType(T.dynamoDbClient)
                    outerScope("identified dynamodb repo") {
                        registerDynamoDb()

                        // register any repo interface too as we typically access those
                        interfaces {
                            filter(Regex("Repository$"))
                            registerDynamoDb()
                        }
                    }
                }
            }
        }

        scope("wire @Scheduled and dynamodb repository entities") {
            listOf(SBAE.command, SBAE.event, SBAE.query)
                .forEach { axon -> E.scheduled["dispatches"] = axon.instantiations }

            listOf(SBAE.eventHandler, SBAE.queryHandler)
                .forEach { handler -> handler["repository-access"] = E.dynamoMethod.invocations }
        }

        scope("graphviz property configuration") {
            // @Scheduled
            graphviz(E.scheduled,
                rank = 0, // endpoints @ rank = 0
                type = Dot.node,
                shape = Shape.box,
            )

            // dynamodb repo and methods
            graphviz(E.dynamoMethod,
                identifyAs = E.dynamoRepository,
                edgeLabel = { readName() }
            )
            graphviz(E.dynamoRepository,
                rank = 4,
                label = replace("Repository", ""),
                type = Dot.node,
                shape = Shape.cylinder
            )
        }
    }

    override fun theme() = springAxon.theme() + mapOf(
        E.client           to plain(light2 + inverse),
        E.scheduled        to plain(light2 + bold),
        E.dynamoRepository to plain(orange1),
        E.dynamoMethod     to plain(orange2),
    )
}

fun main() {
    SandboxTemplate().save()
}