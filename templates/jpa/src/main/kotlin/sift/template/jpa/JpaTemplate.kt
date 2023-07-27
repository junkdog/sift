package sift.template.jpa

import sift.core.dsl.type
import sift.core.entity.Entity
import sift.core.dsl.template
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style.Companion.plain
import sift.template.spi.SystemModelTemplateServiceProvider

typealias E = JpaTemplate.EntityTypes
typealias T = JpaTemplate.AsmTypes

@Suppress("unused")
class JpaTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.jpaMethod, E.jpaRepository)
    override val defaultType: Entity.Type = entityTypes.first()

    object AsmTypes {
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

    override val description: String = """
        |Supporting template providing Spring JpaRepository types.
    """.trimMargin()

    override fun template() = template {
//        synthesize("embed repository interface classes") {
            // level 0; base interface
//            inject(Repository::class)
//            inject(CrudRepository::class)
//            inject(ListCrudRepository::class)
//            inject(PagingAndSortingRepository::class)
//            inject(ListPagingAndSortingRepository::class)
//            inject(JpaRepository::class)
//
            // registers Repository::findByIdOrNull extension function; thereby enabling
            // detection of the incorporated call to Repository::findById
//            inject(Class.forName("org.springframework.data.repository.CrudRepositoryExtensionsKt"))
//        }

        classes {
            scope("register JPA repositories") {
                implements(T.jpaRepository)
                entity(E.jpaRepository)

                // skipping registration of declared jpa methods as additional
                // methods need to be resolved from invocations on the repo anyway.
//                methods(inherited) {
//                    entity(E.jpaMethod)
//                    E.jpaRepository["methods"] = E.jpaMethod
//                }
            }

            methods("resolve JPA methods from usage") {

                // stubbing missing methods for entity registration:
                // we don't have access to inherited jpa repo methods unless
                // the library repo classes are included with the input classes.
                invocationsOf(E.jpaRepository, synthesize = true) {
                    entity(E.jpaMethod)
                    outerScope("repository classes") {
                        E.jpaRepository["methods"] = E.jpaMethod
                    }
                }
            }
        }
    }

    override fun theme() = mapOf(
        E.jpaRepository to plain(orange1),
        E.jpaMethod     to plain(orange2),
    )
}
