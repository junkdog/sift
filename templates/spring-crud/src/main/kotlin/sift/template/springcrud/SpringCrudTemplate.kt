package sift.template.springcrud

import sift.core.dsl.type
import sift.core.entity.Entity
import sift.core.dsl.template
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style
import sift.template.spi.SystemModelTemplateServiceProvider

typealias E = SpringCrudTemplate.EntityTypes
typealias A = SpringCrudTemplate.Annotations
typealias T = SpringCrudTemplate.AsmTypes

@Suppress("unused")
class SpringCrudTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.crudMethod, E.crudRepository)
    override val defaultType: Entity.Type = entityTypes.first()

    object Annotations {
    }

    object AsmTypes {
        val repository = "org.springframework.data.repository.Repository".type
        val crudRepository = "org.springframework.data.repository.CrudRepository".type
        val query = "org.springframework.data.jdbc.repository.query.Query".type
        val modifying = "org.springframework.data.jdbc.repository.query.Modifying".type
    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val crudRepository = "crud-repository".type
        val crudMethod = "crud".type
    }

    override fun create() = this
    override val name: String
        get() = "spring-crud"

    override val description: String = """
        |Supporting template providing Spring Data Repository and CrudRepository types.
    """.trimMargin()

    override fun template() = template {
        classes {
            listOf(T.crudRepository, T.repository).forEach { repository ->
                scope("register CRUD repositories") {
                    implements(repository)
                    entity(E.crudRepository)

                    // skipping registration of declared repo methods as additional
                    // methods need to be resolved from invocations on the repo anyway.
                }

                methods {

                    // stubbing missing methods for entity registration:
                    // we don't have access to inherited repo methods unless
                    // the library repo classes are passed to the pipeline.
                    invocationsOf(E.crudRepository, synthesize = true) {
                        entity(E.crudMethod)
                        outerScope("repository classes") {
                            E.crudRepository["methods"] = E.crudMethod
                        }
                    }
                }
            }
        }
    }

    override fun theme() = mapOf<Entity.Type, Style>(
        E.crudRepository to Style.plain(orange1),
        E.crudMethod     to Style.plain(orange2),
    )
}
