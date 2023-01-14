package sift.instrumenter.sandbox

import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Dsl.instrumenter
import sift.core.terminal.Style
import sift.core.instrumenter.InstrumenterService

typealias E = SandboxInstrumenter.EntityTypes
typealias A = SandboxInstrumenter.Annotations
typealias T = SandboxInstrumenter.AsmTypes

@Suppress("unused")
class SandboxInstrumenter : InstrumenterService {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.jpaMethod, E.jpaRepository)
    override val defaultType: Entity.Type = entityTypes.first()

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

    override val name: String
        get() = "sanbox"

    override fun pipeline() = instrumenter {
        classes {
        }
    }

    override fun theme() = mapOf<Entity.Type, Style>(
    )
}
