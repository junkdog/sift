package sift.template.sift

import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import sift.core.api.Action
import sift.core.api.AccessFlags.acc_public
import sift.core.api.SiftTemplateDsl
import sift.core.dsl.MethodSelection.inherited
import sift.core.dsl.ParameterSelection.excludingReceiver
import sift.core.dsl.template
import sift.core.dsl.type
import sift.core.entity.Entity
import sift.core.terminal.Gruvbox.blue1
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style
import sift.core.terminal.Style.Companion.plain
import sift.core.terminal.TextTransformer.Companion.replace
import sift.core.tree.EntityNode
import sift.core.tree.Tree
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
            // 'sift.core.api.Dsl' for sift < 0.7.0
            listOf("sift.core.dsl", "sift.core.api.Dsl").forEach { dsl ->
                classes {
                    filter(dsl)

                    scope("scopes from annotated classes") {
                        annotatedBy<SiftTemplateDsl>()
                        entity(E.scope, label("\${name}", replace("Dsl.", "")),
                            property("name", readName()))
                    }

                    scope("scopes from children of Core<Element>") {
                        implements(Regex("Core").type)
                        entity(E.scope, label("\${name}", replace("Dsl.", "")), // Dsl. prefix < 0.7.0
                            property("name", readName()))
                    }
                }
            }

            classesOf(E.scope) { e ->
                methods(inherited) {
                    filter(Regex("<init>|^get[A-Z]"), invert = true)
                    filter(Regex("set(Action|CurrentProperty)"), invert = true)
                    filter("\$default", invert = true)
                    filter(acc_public)
                    entity(E.dsl, label("\${icon:}\${name}(\${params:})"),
                        property("name", readName()),
                    )

                    // mark functions used for updating entity properties;
                    // property updating functions return Action over ValueNode:s
                    returns {
                        explodeTypeT("Action<_, _<T>>") {
                            filter("ValueNode")
                            property(E.dsl, "icon", withValue("⚙ "))
                        }
                    }

                    parameters(excludingReceiver) {
                        property(E.dsl, "params", readName(shorten = true))
                        property(E.dsl, "param-types", readType())
                    }
                }
                e["fns"] = E.dsl
            }

//            E.dsl["actions"] = E.action.instantiations
//            E.dsl["actions"] = E.action.fieldAccess
        }
    }

    override fun theme() = mapOf(
        E.scope    to plain(orange1 + bold),
        E.dsl      to DslStyle(orange2 + bold, blue2),
//        E.dsl      to plain(orange2 + bold),
        E.element  to plain(blue1),
        E.action   to plain(blue2),
    )
}

private class DslStyle(val fnStyle: TextStyle, val paramStyle: TextStyle) : Style {

    @Suppress("UNCHECKED_CAST")
    override fun format(
        e: Tree<EntityNode>,
        theme: Map<Entity.Type, Style>
    ): String {
        val fn = (e.value as EntityNode.Entity).entity

        val icon = fn["icon"]?.first()?.toString() ?: ""
        val name = fn["name"]?.first()?.toString()?.let { fnStyle(it) } ?:  ""
        val parameters = (fn["params"] as List<String>? ?: listOf())
            .joinToString(fnStyle(", ")) { paramStyle(it) }

        return "$icon$name${fnStyle("(")}$parameters${fnStyle(")")}"
    }
}