package sift.template.sbacqrs

import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Action
import sift.core.api.Dsl
import sift.core.api.Dsl.template
import sift.core.entity.LabelFormatter
import sift.core.graphviz.Dot
import sift.core.graphviz.Shape
import sift.core.graphviz.Style
import sift.core.product
import sift.core.terminal.Gruvbox.aqua2
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.green2
import sift.core.terminal.Gruvbox.light2
import sift.core.terminal.Gruvbox.purple2
import sift.core.terminal.Gruvbox.yellow2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style.Companion.fromProperty
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.dedupe
import sift.core.terminal.TextTransformer.Companion.replace
import sift.template.dsl.graphviz
import sift.template.dsl.registerInstantiationsOf
import sift.template.spi.SystemModelTemplateServiceProvider

typealias A = SpringBootAxonCqrsTemplate.Annotation
typealias E = SpringBootAxonCqrsTemplate.EntityType

@Suppress("unused")
class SpringBootAxonCqrsTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {

    object Annotation {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

        // axon
        val aggregate = "org.axonframework.spring.stereotype.Aggregate".type
        val aggregateIdentifier = "org.axonframework.modelling.command.AggregateIdentifier".type
        val aggregateMember = "org.axonframework.modelling.command.AggregateMember".type
        val commandHandler = "org.axonframework.commandhandling.CommandHandler".type
        val commandHandlerInterceptor = "org.axonframework.modelling.command.CommandHandlerInterceptor".type
        val entityId = "org.axonframework.modelling.command.EntityId".type
        val eventHandler = "org.axonframework.eventhandling.EventHandler".type
        val eventSourcingHandler = "org.axonframework.eventsourcing.EventSourcingHandler".type
        val processingGroup = "org.axonframework.config.ProcessingGroup".type
        val queryHandler = "org.axonframework.queryhandling.QueryHandler".type

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

        // axon
        val aggregate = "aggregate".type
        val aggregateMember = "aggregate-member".type
        val command = "command".type
        val event = "event".type
        val query = "query".type
        val queryHandler = "query-handler".type
        val commandHandler = "command-handler".type
        val eventSourcingHandler = "event-sourcing-handler".type
        val eventHandler = "event-handler".type
        val projection = "projection".type

        // spring
        val controller = "controller".type
        val endpoint = "endpoint".type
    }

    override val defaultType: Entity.Type = E.controller
    override val entityTypes: Iterable<Entity.Type> = E.types

    override fun create() = this
    override val name: String
        get() = "spring-axon"


    override fun template(): Action<Unit, Unit> {

        fun Dsl.Methods.registerAxonHandlers(
            ownerType: Entity.Type,  // aggregate|projection
            handlerAnnotation: Type, // @(Command|Event|Query)Handler
            handledType: Entity.Type,
            handler: Entity.Type,
        ) {
            annotatedBy(handlerAnnotation)
            entity(handler,
                property("dot-id-as", withValue(ownerType)),
            )

            parameters {
                parameter(0)  // 1st parameter is command|event|query
                property(handler, "type", readType())

                // (re-)register command|event|query entity
                explodeType(synthesize = true) { // class scope of parameter
                    entity(handledType)
                    handledType["received-by"] = handler
                }
            }
        }

        fun Dsl.Methods.registerEndpoints(method: String, httpMethod: Type) {
            scope(method) {
                annotatedBy(httpMethod)
                entity(E.endpoint,
                    label("$method /\${base-path:}\${path:}", dedupe('/')),
                    property("path", readAnnotation(httpMethod, "value"))
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

        fun Dsl.Classes.registerAggregate(
            aggregate: Entity.Type,
            label: LabelFormatter = LabelFormatter.FromElement
        ) {
            entity(aggregate, label)

            methods {
                scope("register command handlers with aggregate") {
                    registerAxonHandlers(aggregate, A.commandHandler, E.command, E.commandHandler)
                    aggregate["commands"] = E.commandHandler
                }

                scope("register event sourcing handlers with aggregate") {
                    registerAxonHandlers(aggregate, A.eventSourcingHandler, E.event, E.eventSourcingHandler)
                    aggregate["events"] = E.eventSourcingHandler
                }
            }
        }

        return template {
            classes {
                fun registerController(controller: Type) {
                    scope("register controllers") {
                        annotatedBy(controller)
                        entity(E.controller)

                        methods {
                            // maps to EntityType.endpoint
                            registerEndpoints("DELETE", A.deleteMapping)
                            registerEndpoints("GET",    A.getMapping)
                            registerEndpoints("PATCH",  A.patchMapping)
                            registerEndpoints("POST",   A.postMapping)
                            registerEndpoints("PUT",    A.putMapping)

                            E.controller["endpoints"] = E.endpoint
                        }
                    }
                }

                registerController(A.controller)
                registerController(A.restController)

                scope("register aggregates") {
                    annotatedBy(A.aggregate)
                    registerAggregate(E.aggregate)

                    scope("register member aggregates") {
                        fields {
                            annotatedBy(A.aggregateMember)
                            signature {
                                explodeTypeT("_<T>") { // e.g. list
                                    registerAggregate(E.aggregateMember,
                                        label("\${aggregate}[\${member}]", replace("Aggregate[", "[")))

                                    property(E.aggregateMember, "member", readName())
                                }
                                explodeTypeT("Map<_, T>") {
                                    registerAggregate(E.aggregateMember,
                                        label("\${aggregate}[\${member}]", replace("Aggregate[", "[")))

                                    property(E.aggregateMember, "member", readName())
                                }
                            }
                        }
                    }

                    property(E.aggregateMember, "aggregate", readName())
                }

                scope("register projections") {
                    fun Dsl.Classes.registerProjections(handler: Type) {
                        methods {
                            annotatedBy(handler)
                            outerScope("identified projection") {
                                entity(E.projection)

                                methods {
                                    scope("register event handlers with projection") {
                                        registerAxonHandlers(E.projection, A.eventHandler, E.event, E.eventHandler)
                                        E.projection["events"] = E.eventHandler
                                    }

                                    scope("register event sourcing handlers with project") {
                                        registerAxonHandlers(E.projection, A.queryHandler, E.query, E.queryHandler)
                                        E.projection["queries"] = E.queryHandler
                                    }
                                }
                            }
                        }
                    }

                    scope("scan suspect classes") {
                        filter("Projection")
                        registerProjections(A.eventHandler)
                        registerProjections(A.queryHandler)
                    }

                    scope("id by @ProcessingGroup") {
                        annotatedBy(A.processingGroup)
                        registerProjections(A.eventHandler)
                        registerProjections(A.queryHandler)
                    }
                }
            }

            scope("scan for sent commands, events and queries") {

                val soughtTypes = listOf(E.command, E.event, E.query)
                val methodsToScan = listOf(
                    E.commandHandler,
                    E.endpoint,
                    E.eventHandler,
                    E.eventSourcingHandler,
                    E.queryHandler,
                )

                soughtTypes.product(methodsToScan)
                    .forEach { (type, methods) -> registerInstantiationsOf(type, methods) }
            }

            scope("dot graph property configuration") {
                // nodes
                graphviz(E.endpoint,
                    rank = 0,
                    type = Dot.node,
                )

                graphviz(listOf(E.aggregate, E.aggregateMember),
                    rank = 1,
                    type = Dot.node,
                    removeSuffix = "Aggregate",
                    shape = Shape.component
                )

                graphviz(E.event,
                    rank = 2,
                    removeSuffix = "Event",
                    shape = Shape.folder,
                    type = Dot.node
                )

                graphviz(E.projection,
                    rank = 3,
                    type = Dot.node,
                    removeSuffix = "Projection",
                    shape = Shape.box3d
                )

                // edges
                graphviz(E.command,
                    type = Dot.edge,
                    removeSuffix = "Command"
                )

                graphviz(E.query,
                    type = Dot.edge,
                    style = Style.dashed,
                    removeSuffix = "Query",
                    arrowheadShape = "onormal",
                )
            }
        }
    }

    override fun theme() = mapOf(
        E.commandHandler       to fromProperty("dot-id-as"),
        E.eventHandler         to fromProperty("dot-id-as"),
        E.eventSourcingHandler to fromProperty("dot-id-as"),
        E.queryHandler         to fromProperty("dot-id-as"),
        E.aggregate            to plain(purple2),
        E.aggregateMember      to plain(purple2),
        E.command              to plain(yellow2 + bold),
        E.controller           to plain(light2 + inverse),
        E.endpoint             to plain(light2 + bold),
        E.event                to plain(aqua2 + bold),
        E.projection           to plain(green2),
        E.query                to plain(blue2),
    )
}
