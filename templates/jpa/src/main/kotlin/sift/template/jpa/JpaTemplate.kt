package sift.template.jpa

import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.api.Dsl.template
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style
import sift.template.spi.SystemModelTemplateServiceProvider

typealias E = JpaTemplate.EntityTypes
typealias A = JpaTemplate.Annotations
typealias T = JpaTemplate.AsmTypes

@Suppress("unused")
class JpaTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.jpaMethod, E.jpaRepository)
    override val defaultType: Entity.Type = entityTypes.first()

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

    override fun template() = template {
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
                    outerScope("repository classes") {
                        E.jpaRepository["methods"] = E.jpaMethod
                    }
                }
            }
        }
    }

    override fun theme() = mapOf<Entity.Type, Style>(
        E.jpaRepository to Style.plain(orange1),
        E.jpaMethod     to Style.plain(orange2),
    )
}
