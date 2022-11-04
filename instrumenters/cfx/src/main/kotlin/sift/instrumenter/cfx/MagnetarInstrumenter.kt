package sift.instrumenter.cfx

import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.*
import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Dsl.instrumenter
import sift.core.api.ScopeEntityPredicate.ifExists
import sift.core.combine
import sift.core.entity.EntityService
import sift.core.product
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.instrumenter.Gruvbox.light0
import sift.instrumenter.Gruvbox.light2
import sift.instrumenter.InstrumenterService
import sift.instrumenter.Style
import sift.instrumenter.Style.Companion.plain
import sift.instrumenter.dsl.buildTree
import sift.instrumenter.dsl.registerInstantiationsOf
import sift.instrumenter.dsl.registerInvocationsOf
import sift.instrumenter.jpa.JpaInstrumenter
import sift.instrumenter.sbacqrs.SpringBootAxonCqrsInstrumenter

typealias A = MagnetarInstrumenter.Annotation
typealias E = MagnetarInstrumenter.EntityType
typealias SE = SpringBootAxonCqrsInstrumenter.EntityType
typealias JE = JpaInstrumenter.EntityTypes


@Suppress("unused", "MemberVisibilityCanBePrivate")
class MagnetarInstrumenter : InstrumenterService {

    object Annotation {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

        val cascadeInterfaceCfx = "com.aags.magnetar.cacofonix.applejack.interfaces.rest.CascadeInterface".type
        val cascadeInterfacePmx = "com.aags.magnetar.panoramix.alderaan.interfaces.rest.cascade.CascadeInterface".type
        val notificationHandler = "magnetar.fulliautomatix.axonspringglue.sns.NotificationHandler".type

    }

    object EntityType {
        internal val types: MutableList<Entity.Type> = mutableListOf()

        private val String.type
            get() = Entity.Type(this).also { types += it }

        val alderaanClient = "alderaan-http-client".type
        val httpClientMethod = "http-client-method".type

        val sns = "sns".type
        val snsHandler = "sns-handler".type
        val snsPublisher = "sns-publisher".type
    }
    val springAxon = SpringBootAxonCqrsInstrumenter()
    val jpa = JpaInstrumenter()

    override val entityTypes: Iterable<Entity.Type> = combine(
        EntityType.types,
        springAxon.entityTypes,
        jpa.entityTypes
    )

    override fun create() = this
    override val name: String
        get() = "magnetar"


    override fun pipeline() = instrumenter {
        include(springAxon.pipeline())
        include(jpa.pipeline())

        synthesize {
            // sns notification classes live in equestria; stubbing
            // placeholders to enable entity tagging
            val notifications = listOf(
                "CallbackNotification",
                "CryptoDomainCreated",
                "DeprovisionCredential",
                "DeprovisionCredentialResponse",
                "KeysetCreatedV2",
                "ProvisioningCompleted",
            ).map { Type.getType("Lmagnetar/cacofonix/equestria/notifications/${it};") }
            notifications.forEach { entity(E.sns, it, label("SNS \${sns-type}")) }
        }

        classesOf(E.sns) {
            update(E.sns, "sns-type", readName())
        }

        // alderaan http clients
        classes {
            filter(Regex("com\\.aags\\.magnetar\\.panoramix\\.alderaan\\.client\\..*Client\$"))
            // only care about the actual clients
            filter(Regex("CascadeClient"), invert = true)
            entity(E.alderaanClient)
            methods {
                log("http client methods")
                filter(Regex("<init>"), invert = true)
                entity(E.httpClientMethod)
            }
        }

        // identify cascade endpoints
        classesOf(SE.controller) {
            listOf(A.cascadeInterfaceCfx, A.cascadeInterfacePmx).forEach { cascadeInterface ->
                scope("identify cascade endpoints") {
                    annotatedBy(cascadeInterface)
                    update(SE.controller, "cascade-interface", withValue(true))
                    methods {
                        filter(SE.endpoint)
                        update(SE.endpoint, "cascade-interface", withValue(true))
                    }
                }
            }

            scope("sns handlers") {
                methods {
                    annotatedBy(A.notificationHandler)
                    entity(E.snsHandler, label("SNS \${sns-type}"))
                    parameters {
                        parameter(0)
                        explodeType(synthesize = true) {
                            entity(E.sns)
                            update(E.snsHandler, "sns-type", readName())
                        }

                        E.snsHandler["backtrack"] = E.sns
                    }

                    scope("conditional instrumentation of sns handlers", ifExists, E.snsHandler) {
                        E.snsHandler["commands"] = SE.command.instantiations
                        E.snsHandler["query"] = SE.query.instantiations
                        E.snsHandler["sns"] = E.sns.instantiations
                        SE.controller["sns-endpoints"] = E.snsHandler
                    }
                }
            }
        }

        // scan these for invocations and instantiations of specific types
        val sources = listOf(
            SE.aggregateCtor,
            SE.commandHandler,
            SE.endpoint,
            SE.eventHandler,
            SE.eventSourcingHandler,
            SE.queryHandler,
        )

        sources
            .forEach { source -> registerInstantiationsOf(E.sns, source) }

        listOf(JE.jpaMethod, E.httpClientMethod)
            .product(sources)
            .forEach { (method, source) -> registerInvocationsOf(method, source) }
    }

    override fun toTree(
        es: EntityService,
        forType: Entity.Type?
    ): Tree<EntityNode> {
        fun Entity.Type.entities(): List<Entity> = es[this].map { (_, entity) -> entity }

        val type = forType ?: SE.controller
        return tree(type.id) {
            type.entities().forEach { e ->
                add(e) {
                    buildTree(e)
                }
            }
        }.also { it.sort(compareBy(EntityNode::toString)) }
    }

    override fun theme() = jpa.theme() + springAxon.theme() + mapOf(
        E.alderaanClient   to plain(light2 + bold, '/'),
        E.httpClientMethod to plain(light2 + bold, '/'),
        E.sns              to plain(light2 + bold),
        E.snsHandler       to plain(light2 + bold),
        SE.controller      to endpointStyle(light2 + inverse, light0 + inverse + italic),
        SE.endpoint        to endpointStyle(light2 + bold,    light0 + bold + italic),
    )
}

fun endpointStyle(
    styling: TextStyle,
    cascadeStyling: TextStyle
): Style = object : Style {
    override fun format(e: Tree<EntityNode>, theme: Map<Entity.Type, Style>): String {
        val text = e.value.toString()
            .replace(Regex("/+"), "/")

        val entity = (e.value as EntityNode.Entity).entity
        val style = entity["cascade-interface"]
            ?.let { cascadeStyling }
            ?: styling

        return style(text)
    }
}