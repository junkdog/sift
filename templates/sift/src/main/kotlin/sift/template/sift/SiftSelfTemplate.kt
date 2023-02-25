package sift.template.sift

import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import org.objectweb.asm.Type
import sift.core.api.Action
import sift.core.api.Dsl
import sift.core.api.Dsl.template
import sift.core.api.SiftTemplateDsl
import sift.core.asm.type
import sift.core.entity.Entity
import sift.core.terminal.Gruvbox.blue1
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style
import sift.core.terminal.Style.Companion.plain
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.template.spi.SystemModelTemplateServiceProvider

typealias E = SiftSelfTemplate.EntityTypes
typealias T = SiftSelfTemplate.AsmTypes

@Suppress("unused")
class SiftSelfTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.scope, E.dsl, E.action, E.element)
    override val defaultType: Entity.Type = entityTypes.first()

    object AsmTypes {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!
    }

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
                filter("sift.core.api.Dsl")

                scope("scopes from annotated classes") {
                    annotatedBy<SiftTemplateDsl>()
                    entity(E.scope, label("\${name}"),
                        property("name", readName(shorten = true)))
                }

                scope("scopes from children of Core<Element>") {
                    implements(type<Dsl.Core<*>>())
                    entity(E.scope, label("\${name}"),
                        property("name", readName(shorten = true)))
                }
            }

            classesOf(E.scope) { e ->
                methods {
                    filter(Regex("<init>|^get[A-Z]"), invert = true)
                    filter(Regex("set(Action|CurrentProperty)"), invert = true)
                    filter("\$default", invert = true)
                    entity(E.dsl, label("\${name}(\${+params:})"),
                        property("name", readName()))

                    parameters {
                        property(E.dsl, "params", readName(shorten = true))
                    }
                }
                e["fns"] = E.dsl
            }

            methodsOf(E.dsl) { e ->
                e["actions"] = E.action.instantiations
            }
        }
    }

    override fun theme() = mapOf(
        E.scope     to plain(orange1 + bold),
        E.dsl       to DslStyle(orange2 + bold, blue2),
        E.element   to plain(blue1),
        E.action    to plain(blue2),
    )
}

private class DslStyle(val fnStyle: TextStyle, val paramStyle: TextStyle) : Style {

    override fun format(
        e: Tree<EntityNode>,
        theme: Map<Entity.Type, Style>
    ): String {
        val fn = (e.value as EntityNode.Entity).entity
        val name = fn["name"]?.first()?.let { fnStyle(it.toString().substringBefore("-")) } ?:  ""
        val params = fn["params"]?.joinToString(fnStyle(", ")) { paramStyle(it.toString()) }  ?: ""
        return "$name${fnStyle("(")}$params${fnStyle(")")}"
    }
}