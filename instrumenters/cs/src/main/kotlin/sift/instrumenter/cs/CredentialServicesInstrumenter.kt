package sift.instrumenter.cs

import com.github.ajalt.mordant.rendering.TextStyles.*
import org.objectweb.asm.Type
import sift.core.api.Action
import sift.core.api.Dsl
import sift.core.api.Dsl.instrumenter
import sift.core.asm.simpleName
import sift.core.combine
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.product
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.instrumenter.Gruvbox.blue2
import sift.instrumenter.Gruvbox.green2
import sift.instrumenter.Gruvbox.light2
import sift.instrumenter.Gruvbox.purple2
import sift.instrumenter.Gruvbox.yellow2
import sift.instrumenter.InstrumenterService
import sift.instrumenter.Style.Companion.plain
import sift.instrumenter.dsl.buildTree
import sift.instrumenter.dsl.registerInstantiationsOf
import sift.instrumenter.dsl.registerInvocationsOf
import sift.instrumenter.jdbi.Jdbi3Instrumenter

typealias E = CredentialServicesInstrumenter.EntityTypes
typealias A = CredentialServicesInstrumenter.Annotations
typealias T = CredentialServicesInstrumenter.AsmTypes
typealias JE = Jdbi3Instrumenter.EntityTypes

@Suppress("unused")
class CredentialServicesInstrumenter : InstrumenterService {

    object Annotations {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

        // rest
        val httpGet = "javax.ws.rs.GET".type
        val httpPost = "javax.ws.rs.POST".type
        val httpDelete = "javax.ws.rs.DELETE".type
        val httpPut = "javax.ws.rs.PUT".type
        val httpPath = "javax.ws.rs.Path".type
        val produces = "javax.ws.rs.Produces".type
        val consumes = "javax.ws.rs.Consumes".type

        val rolesAllowed = "javax.annotation.security.RolesAllowed".type

        // command handling
        val subscribe = "com.google.common.eventbus.Subscribe".type
    }

    object AsmTypes {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

        // interface
        val stateUpdater = "com.assaabloy.stg.msf.credentialservices.domain.StateUpdater".type
        val mkGateway = "com.assaabloy.stg.msf.credentialservices.gateway.MobileKeysGateway".type
    }

    object EntityTypes {
        internal val types: MutableList<Entity.Type> = mutableListOf()

        private val String.type
            get() = Entity.Type(this).also { types += it }

        // rest
        val controller = "controller".type
        val endpoint = "endpoint".type

        // dao
        val stateUpdater = "state-updated".type

        // remnants
        val command = "command".type
        val commandHandler = "command-handler".type
        val subscriber = "subscriber".type

        val mkGateway = "mobile-keys-gateway".type
    }

    val jdbi3 = Jdbi3Instrumenter()

    override val entityTypes: Iterable<Entity.Type> = combine(E.types, jdbi3.entityTypes)

    override fun create() = this
    override val name = "cs"

    override fun pipeline(): Action<Unit, Unit> {
        fun Dsl.Methods.registerEndpoints(httpMethod: Type) {
            scope(httpMethod.simpleName) {
                annotatedBy(httpMethod)
                entity(
                    E.endpoint, label("${httpMethod.simpleName} /\${base-path:}\${path:}"),
                    property("path", readAnnotation(A.httpPath, "value")))

                parentScope("read base path from class-level @Path") {
                    update(E.endpoint, "base-path", readAnnotation(A.httpPath, "value"))
                }
            }
        }

        return instrumenter {
            // registering @SqlQuery and @SqlUpdate entities
            include(jdbi3.pipeline())

            classes {
                logCount("inspecting")
                scope("register controllers") {
                    annotatedBy(A.httpPath)
                    entity(E.controller)
                    logCount("controllers")

                    methods {
                        registerEndpoints(A.httpDelete)
                        registerEndpoints(A.httpGet)
                        registerEndpoints(A.httpPost)
                        registerEndpoints(A.httpPut)

                        E.controller["endpoints"] = E.endpoint
                    }
                }

                scope("register mobilekeys gateways") {
                    implements(T.mkGateway)
                    methods {
                        filter(Regex("lambda|<init>|<clinit>|format"), invert = true)
                        entity(E.mkGateway)
                    }
                }

                scope("register command @Subscriber methods") {
                    filter(Regex("CommandHandler\$"))

                    methods {
                        annotatedBy(A.subscribe)
                        entity(E.subscriber)
                        logCount("command subscribers")

                        parentScope("re-register command handler") {
                            entity(E.commandHandler)
                        }
                        E.commandHandler["subscribers"] = E.subscriber

                        parameters {
                            parameter(0) // subscribed command type
                            explodeType {
                                entity(E.command)
                                E.command["received-by"] = E.subscriber
                            }
                        }
                    }
                }

                scope("register StateUpdaters") {
                    implements(T.stateUpdater)
                    entity(E.stateUpdater)

                    methods {
                        instantiationsOf(E.command) {
                            E.command["backtrack"] = E.stateUpdater
                            E.stateUpdater["sends"] = E.command
                        }

                        listOf(JE.sqlQuery, JE.sqlUpdate).forEach { sql ->
                            invocationsOf(sql) {
                                sql["backtrack"] = E.stateUpdater
                                E.stateUpdater["sends"] = sql
                            }
                        }
                    }
                }
            }

            scope("register entity relationships from invocations and instantiations") {
                listOf(E.command, E.stateUpdater)
                    .product(listOf(E.subscriber, E.endpoint))
                    .forEach { (type, source) -> registerInstantiationsOf(type, source) }

                listOf(JE.sqlQuery, JE.sqlUpdate)
                    .product(listOf(E.subscriber, E.endpoint))
                    .forEach { (sql, source) -> registerInvocationsOf(sql, source) }

                // todo: E.stateUpdater
                listOf(E.endpoint, E.subscriber)
                    .forEach { parent -> registerInvocationsOf(E.mkGateway, parent) }
            }
        }
    }

    override fun theme() = jdbi3.theme() + mapOf(
        E.subscriber to plain(green2),
        E.command to plain(yellow2 + bold),
        E.controller to plain(light2 + inverse),
        E.endpoint to plain(light2 + bold, '/'),
        E.commandHandler to plain(purple2),
        E.stateUpdater to plain(blue2),
        E.mkGateway to plain(blue2 + bold + underline),
    )

    override fun toTree(
        es: EntityService,
        forType: Entity.Type?
    ): Tree<EntityNode> {
        fun Entity.Type.entities(): List<Entity> = es[this].map { (_, entity) -> entity }

        val type = forType ?: E.controller

        return tree(type.id) {
            type.entities().forEach { e ->
                add(e) {
                    buildTree(e)
                }
            }
        }.also { it.sort(compareBy(EntityNode::toString)) }
    }
}
