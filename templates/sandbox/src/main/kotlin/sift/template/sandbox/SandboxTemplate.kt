package sift.template.sandbox

import sift.core.dsl.template
import sift.core.entity.Entity
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style

typealias E = SandboxTemplate.EntityTypes
typealias A = SandboxTemplate.Annotations
typealias T = SandboxTemplate.AsmTypes

@Suppress("unused")
class SandboxTemplate : SystemModelTemplate {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.jpaMethod, E.jpaRepository)
    override val defaultType: Entity.Type = entityTypes.first()

    object Annotations {
    }

    object AsmTypes {
    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val jpaRepository = "jpa-repository".type
        val jpaMethod = "jpa".type
    }

    override val name: String
        get() = "sanbox"

    override val description: String = "playground"

    override fun template() = template {
        classes {
        }
    }

    override fun theme() = mapOf<Entity.Type, Style>(
    )
}
