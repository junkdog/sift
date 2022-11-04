package sift.instrumenter.jpa

import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Dsl.instrumenter
import sift.core.entity.EntityService
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.instrumenter.Gruvbox.orange1
import sift.instrumenter.Gruvbox.orange2
import sift.instrumenter.InstrumenterService
import sift.instrumenter.Style
import sift.instrumenter.dsl.buildTree

typealias E = JpaInstrumenter.EntityTypes
typealias A = JpaInstrumenter.Annotations
typealias T = JpaInstrumenter.AsmTypes

@Suppress("unused")
class JpaInstrumenter : InstrumenterService {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.jpaMethod, E.jpaRepository)

    object Annotations {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!
    }

    object AsmTypes {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

        val jpaRepository = "org.springframework.data.jpa.repository.JpaRepository".type
    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val jpaRepository = "jpa-repository".type
        val jpaMethod = "jpa".type
    }

    override fun create() = this
    override val name: String
        get() = "jpa"

    override fun pipeline() = instrumenter {
        classes {
            scope("register JPA repositories") {
                implements(T.jpaRepository)
                entity(E.jpaRepository)
                log("jpa repos")

                // skipping registration of declared jpa methods as additional
                // methods need to be resolved from invocations on the repo anyway.
            }

             methods {

                // stubbing missing methods for entity registration:
                // we don't have access to inherited jpa repo methods unless
                // the library repo classes are passed to the pipeline.
                invocationsOf(E.jpaRepository, synthesize = true) {
                    entity(E.jpaMethod)
                    log("jpa methods")
                    parentScope("repository classes") {
                        E.jpaRepository["methods"] = E.jpaMethod
                    }
                }
            }
        }
    }

    override fun toTree(
        es: EntityService,
        forType: Entity.Type?
    ): Tree<EntityNode> {
        fun Entity.Type.entities(): List<Entity> = es[this].map { (_, entity) -> entity }

        val type = forType ?: E.jpaRepository
        return tree(type.id) {
            type.entities().forEach { e ->
                add(e) {
                    buildTree(e)
                }
            }
        }.also { it.sort(compareBy(EntityNode::toString)) }
    }

    override fun theme() = mapOf<Entity.Type, Style>(
        E.jpaRepository to Style.plain(orange1),
        E.jpaMethod     to Style.plain(orange2),
    )
}
