package sift.template.sbacqrs

import com.github.ajalt.mordant.rendering.TextStyles.bold
import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Action
import sift.core.dsl.Classes
import sift.core.dsl.Methods
import sift.core.dsl.template
import sift.core.entity.LabelFormatter
import sift.core.graphviz.Dot
import sift.core.graphviz.Shape
import sift.core.graphviz.Style
import sift.core.product
import sift.core.terminal.Gruvbox.aqua2
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.green2
import sift.core.terminal.Gruvbox.purple2
import sift.core.terminal.Gruvbox.yellow2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style.Companion.fromProperty
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.replace
import sift.template.dsl.graphviz
import sift.template.spi.SystemModelTemplateServiceProvider
import sift.template.springboot.SpringBootTemplate


typealias SBA = SpringBootTemplate.Annotation
typealias SBE = SpringBootTemplate.EntityType

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
    }

    val springBoot = SpringBootTemplate()

    override val defaultType: Entity.Type = SBE.controller
    override val entityTypes: Iterable<Entity.Type> = springBoot.entityTypes + E.types

    override fun create() = this
    override val name: String
        get() = "spring-axon"

    override val description: String = "Spring Boot + Axon CQRS applications"

    override fun template(): Action<Unit, Unit> {

        fun Methods.registerAxonHandlers(
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

        fun Classes.registerAggregate(
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
            include(springBoot.template())

            classes {
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
                    fun Classes.registerProjections(handler: Type) {
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

            scope("wire commands, events and queries") {

                val soughtTypes = listOf(E.command, E.event, E.query)
                val methodsToScan = listOf(
                    E.commandHandler,
                    SBE.endpoint,
                    E.eventHandler,
                    E.eventSourcingHandler,
                    E.queryHandler,
                )

                methodsToScan.product(soughtTypes)
                    .forEach { (method, type) -> method["instantiates"] = type.instantiations }
            }

            scope("dot graph property configuration") {
                graphviz(listOf(E.aggregate, E.aggregateMember),
                    rank = 1, // endpoints @ rank = 0
                    type = Dot.node,
                    shape = Shape.component,
                    label = replace(Regex("Aggregate\$"), ""),
                )

                graphviz(E.event,
                    rank = 2,
                    shape = Shape.folder,
                    type = Dot.node,
                    label = replace(Regex("Event\$"), ""),
                )

                graphviz(E.projection,
                    rank = 3,
                    type = Dot.node,
                    label = replace(Regex("Projection\$"), ""),
                    shape = Shape.box3d
                )

                // edges
                graphviz(E.command,
                    type = Dot.edge,
                    label = replace(Regex("Command\$"), "")
                )

                graphviz(
                    E.query,
                    type = Dot.edge,
                    style = Style.dashed,
                    label = replace(Regex("Query\$"), ""),
                    arrowheadShape = "onormal",
                )
            }
        }
    }

    override fun theme() = springBoot.theme() + mapOf(
        E.commandHandler       to fromProperty("dot-id-as"),
        E.eventHandler         to fromProperty("dot-id-as"),
        E.eventSourcingHandler to fromProperty("dot-id-as"),
        E.queryHandler         to fromProperty("dot-id-as"),
        E.aggregate            to plain(purple2),
        E.aggregateMember      to plain(purple2),
        E.command              to plain(yellow2 + bold),
        E.event                to plain(aqua2 + bold),
        E.projection           to plain(green2),
        E.query                to plain(blue2),
    )
}
