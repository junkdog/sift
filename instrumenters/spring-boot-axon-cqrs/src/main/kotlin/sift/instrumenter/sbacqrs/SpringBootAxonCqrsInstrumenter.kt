package sift.instrumenter.sbacqrs

import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.inverse
import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Action
import sift.core.api.Dsl
import sift.core.api.Dsl.instrumenter
import sift.instrumenter.graphviz.Dot
import sift.core.product
import sift.instrumenter.Gruvbox.aqua2
import sift.instrumenter.Gruvbox.blue2
import sift.instrumenter.Gruvbox.green2
import sift.instrumenter.Gruvbox.light2
import sift.instrumenter.Gruvbox.purple2
import sift.instrumenter.Gruvbox.yellow2
import sift.instrumenter.InstrumenterService
import sift.instrumenter.Style.Companion.fromProperty
import sift.instrumenter.Style.Companion.plain
import sift.instrumenter.dsl.registerInstantiationsOf

typealias A = SpringBootAxonCqrsInstrumenter.Annotation
typealias E = SpringBootAxonCqrsInstrumenter.EntityType

@Suppress("unused")
class SpringBootAxonCqrsInstrumenter : InstrumenterService {

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
        val aggregateCtor = "aggregate-ctor".type // explicit aggregate creation
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


    override fun pipeline(): Action<Unit, Unit> {

        fun Dsl.Methods.registerAxonHandlers(
            ownerType: Entity.Type,  // aggregate|projection
            handlerAnnotation: Type, // @(Command|Event|Query)Handler
            handledType: Entity.Type,
            handler: Entity.Type,
            handledDotType: Dot = Dot.edge
        ) {
            annotatedBy(handlerAnnotation)
            entity(handler,
                property("dot-id", withValue(ownerType)),
            )

            parameters {
                parameter(0)  // 1st parameter is command|event|query
                update(handler, "type", readType())

                // (re-)register command|event|query entity
                explodeType(synthesize = true) { // class scope of parameter
                    entity(handledType,
                        property("dot-type", withValue(handledDotType)))
                    handledType["received-by"] = handler
                }
            }
        }

        fun Dsl.Methods.registerEndpoints(method: String, httpMethod: Type) {
            scope(method) {
                annotatedBy(httpMethod)
                entity(E.endpoint, label("$method /\${base-path:}\${path:}"),
                    property("path", readAnnotation(httpMethod, "value")))

                parentScope("read base path from @RequestMapping") {
                    update(E.endpoint, "base-path", readAnnotation(A.requestMapping, "value"))
                }

                parameters {
                    annotatedBy(A.requestBody)
                    update(E.endpoint, "request-object", readType())
                }
            }
        }

        fun Dsl.Classes.registerAggregate(aggregate: Entity.Type) {
            entity(aggregate)
            methods {
                scope("register command handlers with aggregate") {
                    registerAxonHandlers(aggregate, A.commandHandler, E.command, E.commandHandler)
                    aggregate["commands"] = E.commandHandler
                }

                scope("register event sourcing handlers with aggregate") {
                    registerAxonHandlers(aggregate, A.eventSourcingHandler, E.event, E.eventSourcingHandler, Dot.node)
                    aggregate["events"] = E.eventSourcingHandler
                }

                scope("register explicit aggregate creation via constructor") {
                    filter("<init>".toRegex())
                    entity(E.aggregateCtor, errorIfExists = false)
                }
            }
        }

        return instrumenter {
            classes {
                scope("register controllers") {
                    annotatedBy(A.restController)
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

                scope("register aggregates") {
                    annotatedBy(A.aggregate)
                    registerAggregate(E.aggregate)
                }

                scope("register member aggregates") {
                    fields {
                        annotatedBy(A.entityId)
                        parentScope("member aggregate") {
                            registerAggregate(E.aggregateMember)
                        }
                    }
                }

                scope("register projections") {
                    annotatedBy(A.processingGroup)
                    entity(E.projection)

                    methods {
                        scope("register event handlers with aggregate") {
                            registerAxonHandlers(E.projection, A.eventHandler, E.event, E.eventHandler, Dot.node)
                            E.projection["events"] = E.eventHandler
                        }

                        scope("register event sourcing handlers with aggregate") {
                            registerAxonHandlers(E.projection, A.queryHandler, E.query, E.queryHandler)
                            E.projection["queries"] = E.queryHandler
                        }
                    }
                }
            }

            scope("scan for sent commands, events and queries") {

                val soughtTypes = listOf(E.command, E.event, E.query)
                val methodsToScan = listOf(
                    E.aggregateCtor,
                    E.commandHandler,
                    E.endpoint,
                    E.eventHandler,
                    E.eventSourcingHandler,
                    E.queryHandler,
                )

                soughtTypes.product(methodsToScan)
                    .forEach { (type, methods) -> registerInstantiationsOf(type, methods) }

                methodsOf(E.commandHandler) {
                    E.commandHandler["sends"] = E.aggregateCtor.invocations
                }
            }

            scope("dot graph property configuration") {
                fun rankMn(e: Entity.Type, rank: Int) {
                    methodsOf(e) {
                        update(e, "dot-rank", withValue(rank))
                        update(e, "dot-type", withValue(Dot.node))
                    }
                }

                fun rankCn(e: Entity.Type, rank: Int) {
                    classesOf(e) {
                        update(e, "dot-rank", withValue(rank))
                        update(e, "dot-type", withValue(Dot.node))
                    }
                }

                fun stripSuffix(e: Entity.Type, suffix: String) {
                    classesOf(e) {
                        update(e, "dot-label-strip", withValue(suffix))
                    }
                }

                rankMn(E.endpoint, 0)
                rankCn(E.aggregate, 1)
                rankCn(E.event, 2)
                rankCn(E.projection, 3)

                stripSuffix(E.command, "Command")
                stripSuffix(E.event, "Event")
                stripSuffix(E.query, "Query")
                stripSuffix(E.aggregate, "Aggregate")
                stripSuffix(E.projection, "Projection")
            }
        }
    }

    override fun theme() = mapOf(
        E.commandHandler       to fromProperty("owner-type"),
        E.eventHandler         to fromProperty("owner-type"),
        E.eventSourcingHandler to fromProperty("owner-type"),
        E.queryHandler         to fromProperty("owner-type"),
        E.aggregate            to plain(purple2),
        E.aggregateMember      to plain(purple2),
        E.command              to plain(yellow2 + bold),
        E.controller           to plain(light2 + inverse),
        E.endpoint             to plain(light2 + bold, '/'),
        E.event                to plain(aqua2 + bold),
        E.projection           to plain(green2),
        E.query                to plain(blue2),
    )
}
