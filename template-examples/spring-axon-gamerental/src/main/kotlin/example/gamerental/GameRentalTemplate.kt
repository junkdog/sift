package example.gamerental

import sift.core.api.type
import sift.core.entity.Entity
import sift.core.dsl.template
import sift.core.graphviz.Dot
import sift.core.graphviz.Shape
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Gruvbox.light2
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.replace
import sift.template.dsl.graphviz
import sift.template.jpa.JpaTemplate
import sift.template.sbacqrs.SpringBootAxonCqrsTemplate
import sift.template.springboot.SpringBootTemplate

// in the interest of brevity and organization
typealias E = GameRentalTemplate.EntityTypes
typealias A = GameRentalTemplate.Annotations

typealias JE = JpaTemplate.EntityTypes
typealias SBE = SpringBootTemplate.EntityType
typealias SBAE = SpringBootAxonCqrsTemplate.EntityType

/**

 */
@Suppress("unused")
class GameRentalTemplate : SystemModelTemplate {
    // extending the following baseline templates; they provide entities and styling
    val springAxon = SpringBootAxonCqrsTemplate()
    val jpa = JpaTemplate()

    override val entityTypes: Iterable<Entity.Type> =
        springAxon.entityTypes + jpa.entityTypes + listOf(E.rsocket)

    // root type to build the tree/graph from; override with --tree-root ENTITY_TYPE
    override val defaultType: Entity.Type = springAxon.defaultType

    // Annotations, AsmTypes and EntityTypes are just for organization
    object Annotations {
        // spring
        val messageMapping = "org.springframework.messaging.handler.annotation.MessageMapping".type
    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val rsocket = "rsocket".type
    }

    /** `sift --template gamerental ...` */
    override val name: String
        get() = "gamerental"
    override val description: String
        get() = "project-specific template showcasing spring boot, axon, JPA and RSocket endpoints"

    override fun template() = template {
        include(springAxon.template()) // entities: controllers, endpoints, cqrs
        include(jpa.template())        // entities: repositories

        scope("register @MessageMapping endpoints") {
            classesOf(SBE.controller) {
                methods {
                    annotatedBy(A.messageMapping)
                    entity(E.rsocket, label("\uD83D\uDD0C /\${name}"),
                        property("name", readAnnotation(A.messageMapping, "value"))
                    )

                    // associate controllers as parents of rsocket endpoints
                    SBE.controller["rsockets"] = E.rsocket
                }
            }
        }

        scope("wire @MessageMapping and JPA entities") {

            // rsocket endpoints may send commands and queries; id these from instantiations.
            listOf(SBAE.command, SBAE.query, SBAE.event)
                .forEach { cqrs -> E.rsocket["instantiates"] = cqrs.instantiations }

            // jpa repositories can potentially be invoked from everywhere;
            // methods are scanned recursively for JPA method invocations.
            listOf(
                E.rsocket,                     // @MessageMapping endpoints
                SBE.endpoint,                  // @(Post|Get|Put|Delete)Mapping endpoints
                SBAE.commandHandler,           // @CommandHandler methods
                SBAE.eventHandler,             // @EventHandler methods
                SBAE.queryHandler              // @QueryHandler methods
            ).forEach { method -> method["invokes"] = JE.jpaMethod.invocations }
        }

        // for `--render`: updates entities with additional graphviz properties.
        // the `spring-axon` template is already configured, so only new entities
        // require configuration.
        scope("graphviz configuration") {
            graphviz(E.rsocket,
                rank = 0,                      // share rank with endpoint
                type = Dot.node
            )

            // render JPA methods as labeled edges pointing to their repository.
            graphviz(JE.jpaMethod,
                identifyAs = JE.jpaRepository, // render as the repository entity instead
                edgeLabel = { readName() },    // invoked repo method name
            )

            // style it more database-y
            graphviz(JE.jpaRepository,
                rank = 4,                      // projections.rank = 3
                type = Dot.node,
                shape = Shape.cylinder,
                label = listOf(replace("Repository", ""))
            )
        }
    }

    override fun theme() = springAxon.theme() + jpa.theme() + mapOf(
        E.rsocket to plain(light2)
    )
}
