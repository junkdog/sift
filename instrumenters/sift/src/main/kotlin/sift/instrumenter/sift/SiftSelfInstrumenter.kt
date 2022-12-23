package sift.instrumenter.sift

import com.github.ajalt.mordant.rendering.TextStyles.bold
import org.objectweb.asm.Type
import sift.core.api.Action
import sift.core.api.Dsl
import sift.core.api.Dsl.instrumenter
import sift.core.api.SiftTemplateDsl
import sift.core.asm.type
import sift.core.entity.Entity
import sift.core.terminal.Gruvbox.blue1
import sift.core.terminal.Gruvbox.blue2
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.instrumenter.InstrumenterService
import sift.core.terminal.Style
import sift.core.terminal.Style.Companion.plain
import sift.instrumenter.spi.InstrumenterServiceProvider
import java.util.regex.Pattern

typealias E = SiftSelfInstrumenter.EntityTypes
typealias T = SiftSelfInstrumenter.AsmTypes

@Suppress("unused")
class SiftSelfInstrumenter : InstrumenterService, InstrumenterServiceProvider {
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

    override fun pipeline() = instrumenter {
        scope("register actions") {
            classes {
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
                filter(Pattern.quote("sift.core.api.Dsl").toRegex())

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

            classesOf(E.scope) {
                methods {
                    filter(Regex("<init>|^get[A-Z]"), invert = true)
                    filter(Regex("set(Action|CurrentProperty)"), invert = true)
                    filter(Pattern.quote("\$default").toRegex(), invert = true)
                    entity(E.dsl, label("\${name}(\${+params:})"),
                        property("name", readName()))

                    parameters {
                        property(E.dsl, "params", readName(shorten = true))
                    }
                }
                E.scope["fns"] = E.dsl
            }

            methodsOf(E.dsl) {
                E.dsl["actions"] = E.action.instantiations
            }
        }
    }

    override fun theme() = mapOf<Entity.Type, Style>(
        E.scope     to plain(orange1 + bold),
        E.dsl       to plain(orange2 + bold),
        E.element   to plain(blue1),
        E.action    to plain(blue2),
    )
}
