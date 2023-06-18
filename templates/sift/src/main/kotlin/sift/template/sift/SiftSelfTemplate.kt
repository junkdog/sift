package sift.template.sift

import com.github.ajalt.mordant.rendering.TextStyles.bold
import sift.core.api.Action
import sift.core.api.AccessFlags.acc_public
import sift.core.api.AccessFlags.acc_synthetic
import sift.core.api.SiftTemplateDsl
import sift.core.dsl.*
import sift.core.dsl.MethodSelection.declared
import sift.core.dsl.MethodSelection.inherited
import sift.core.dsl.ParameterSelection.excludingReceiver
import sift.core.dsl.Visibility.Public
import sift.core.entity.Entity
import sift.core.terminal.Gruvbox.blue1
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.replace
import sift.core.terminal.TextTransformer.Companion.stylize
import sift.template.spi.SystemModelTemplateServiceProvider

typealias E = SiftSelfTemplate.EntityTypes

@Suppress("unused")
class SiftSelfTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.scope, E.dsl, E.action, E.element)
    override val defaultType: Entity.Type = entityTypes.first()

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val action = "action".type
        val dsl = "dsl".type
        val element = "element".type
        val scope = "scope".type
    }

    override fun create() = this
    override val name: String
        get() = "sift"

    override val description: String = """
        |Introspects DSL API. Can be used to diff API between versions.
    """.trimMargin()


    override fun template() = template {
        scope("register actions") {
            classes {
                filter("sift.core.api")

                implements(type<Action<*, *>>())
                filter(Regex("SimpleAction"), invert = true)
                entity(E.action )

                outerScope("scope") {
                    entity(E.element)
                }
                E.element["actions"] = E.action
            }
        }


        scope("register dsl") {
            classes {
                // 'sift.core.api.Dsl' for sift < 0.7.0
                filter(Regex("sift\\.core\\.(dsl|api\\.Dsl)"))

                scope("scopes from annotated classes") {
                    annotatedBy<SiftTemplateDsl>()
                    entity(E.scope, label("\${name}"),
                        property("name", readName() andThen(replace("Dsl.", ""))))
                }

                scope("scopes from children of Core<Element>") {
                    implements(Regex("Core").type)
                    entity(E.scope, label("\${name}"), // Dsl. prefix < 0.7.0
                        property("name", readName() andThen replace("Dsl.", "")))
                }
            }

            classesOf(E.scope) { scope ->
                methods(inherited) {
                    filter(Public)

                    // dsl functions per scope
                    entity(E.dsl, label("\${icon:}\${name}(\${params:})"),
                        property("name", readName()),
                    )

                    // mark functions used for updating entity properties;
                    // property updating functions output Action over ValueNode:s
                    returns {
                        explodeTypeT("Action<_, _<T>>") {
                            filter("ValueNode")
                            // every dsl property function is henceforth associated with ValueNode;
                            // it would be prudent to maybe find another approach in the future,
                            // which only inspected w/o relating... but works for now (yolo)
                            property(E.dsl, "icon", withValue("⚙ "))
                        }
                    }

                    parameters(excludingReceiver) {
                        property(E.dsl, "params", readName() andThen stylize(blue2))
                        property(E.dsl, "param-types", readType())
                    }
                }
                scope["fns"] = E.dsl
            }

//            E.dsl["actions"] = E.action.instantiations
//            E.dsl["actions"] = E.action.fieldAccess
        }
    }

    override fun theme() = mapOf(
        E.scope    to plain(orange1 + bold),
        E.dsl      to plain(orange2 + bold),
        E.element  to plain(blue1),
        E.action   to plain(blue2),
    )
}