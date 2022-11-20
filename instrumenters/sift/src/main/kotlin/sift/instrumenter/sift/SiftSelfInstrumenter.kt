package sift.instrumenter.sift

import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.TextStyles.bold
import org.objectweb.asm.Type
import sift.core.api.Action
import sift.core.api.Dsl
import sift.core.entity.Entity
import sift.core.api.Dsl.instrumenter
import sift.core.api.SiftTemplateDsl
import sift.core.asm.type
import sift.core.graphviz.Dot
import sift.instrumenter.Gruvbox
import sift.instrumenter.Gruvbox.blue1
import sift.instrumenter.Gruvbox.blue2
import sift.instrumenter.Gruvbox.orange1
import sift.instrumenter.Gruvbox.orange2
import sift.instrumenter.Gruvbox.yellow1
import sift.instrumenter.InstrumenterService
import sift.instrumenter.Style
import sift.instrumenter.Style.Companion.plain
import sift.instrumenter.serialize
import java.io.File
import java.util.regex.Pattern

typealias E = SiftSelfInstrumenter.EntityTypes
typealias T = SiftSelfInstrumenter.AsmTypes

@Suppress("unused")
class SiftSelfInstrumenter : InstrumenterService {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.dsl, E.action, E.scope)
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
        val parameter = "parameter".type
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
                filter(Regex("""sift\.core\.api\.Dsl"""))

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
                    filter(Regex("setAction"), invert = true)
//                    filterName(Pattern.quote("$").toRegex(), invert = true)
                    entity(E.dsl, label("\${name}(\${+params:})"),
                        property("name", readName()))

                    parameters {
                        property(E.dsl, "params", readName(shorten = true))
                        entity(E.parameter, label("\${name}: \${type}"),
                            property("name", readName()))

                        explodeType(synthesize = true) {
                            property(E.parameter, "type", readName())
                        }
                    }

                    E.dsl["params"] = E.parameter
                }
                E.scope["fns"] = E.dsl
            }

            methodsOf(E.dsl) {
                E.dsl["actions"] = E.action.instantiations
            }
        }

        scope("dot graph property configuration") {
            fun rankMn(e: Entity.Type, rank: Int) {
                methodsOf(e) {
                    property(e, "dot-rank", withValue(rank))
                    property(e, "dot-type", withValue(Dot.node))
                }
            }

            fun rankCn(e: Entity.Type, rank: Int) {
                classesOf(e) {
                    property(e, "dot-rank", withValue(rank))
                    property(e, "dot-type", withValue(Dot.node))
                }
            }

            rankCn(E.scope, 0)
            rankMn(E.dsl, 1)
            rankCn(E.action, 2)
            rankCn(E.element, 3)
        }
    }

    override fun theme() = mapOf<Entity.Type, Style>(
        E.scope     to plain(orange1 + bold),
        E.dsl       to plain(orange2 + bold),
        E.element   to plain(blue1),
        E.action    to plain(blue2),
        E.parameter to plain(yellow1),
    )
}

fun save(instrumenter: InstrumenterService) {
    File("${System.getProperty("user.home")}/.local/share/sift/instrumenters")
        .also(File::mkdirs)
        .resolve("${instrumenter.name}.json")
        .writeText(instrumenter.serialize())

    println("installed --instrumenter ${instrumenter.name}")
}

fun main(args: Array<String>) {
    save(SiftSelfInstrumenter())
}
