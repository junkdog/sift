package sift.instrumenter.sandbox

import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Dsl.instrumenter
import sift.core.entity.EntityService
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.instrumenter.InstrumenterService
import sift.instrumenter.Style
import sift.instrumenter.dsl.buildTree

typealias E = SandboxInstrumenter.EntityTypes
typealias A = SandboxInstrumenter.Annotations
typealias T = SandboxInstrumenter.AsmTypes

@Suppress("unused")
class SandboxInstrumenter : InstrumenterService {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.jpaMethod, E.jpaRepository)

    object Annotations {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!
    }

    object AsmTypes {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val jpaRepository = "jpa-repository".type
        val jpaMethod = "jpa".type
    }

    override fun create() = this
    override val name: String
        get() = "sanbox"

    override fun pipeline() = instrumenter {
        classes {
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
    )
}
