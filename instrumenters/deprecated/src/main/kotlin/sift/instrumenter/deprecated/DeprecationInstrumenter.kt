package sift.instrumenter.deprecated

import sift.core.entity.Entity
import sift.core.api.Action
import sift.core.api.Dsl
import sift.core.api.Dsl.classes
import sift.core.api.SystemModel
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.instrumenter.InstrumenterService
import sift.core.terminal.Style

typealias JavaDeprecated = java.lang.Deprecated

@Suppress("unused")
class DeprecationInstrumenter : InstrumenterService {
    val klazz = Entity.Type("classes")
    val referencing = Entity.Type("classes with deprecations")
    val method = Entity.Type("methods")
    val field = Entity.Type("fields")

    override val entityTypes: Iterable<Entity.Type> = listOf(klazz, method, field, referencing)
    override val defaultType: Entity.Type = entityTypes.first()

    override fun create() = this
    override val name: String
        get() = "deprecations"

    override fun pipeline(): Action<Unit, Unit> {
        fun Dsl.Methods.registerWithParent() {
            outerScope("tag class") { entity(referencing) }
            referencing["deprecated"] = method
        }

        fun Dsl.Fields.registerWithParent() {
            outerScope("tag class") { entity(referencing) }
            referencing["deprecated"] = field
        }

        return classes {

            scope("deprecated java classes") {
                annotatedBy<JavaDeprecated>()
                entity(klazz)
            }
            scope("deprecated kotlin classes") {
                annotatedBy<Deprecated>()
                entity(klazz)
            }

            fields {
                annotatedBy<JavaDeprecated>()
                entity(field)
                registerWithParent()
            }
            fields {
                annotatedBy<Deprecated>()
                entity(field)
                registerWithParent()
            }

            methods {
                annotatedBy<JavaDeprecated>()
                entity(method)
                registerWithParent()
            }
            methods {
                annotatedBy<Deprecated>()
                entity(method)
                registerWithParent()
            }
        }
    }

    override fun toTree(
        sm: SystemModel,
        forType: Entity.Type?
    ): Tree<EntityNode> {
        return tree("deprecations") {
            if (klazz in sm)
                add(klazz.id) { sm[klazz].forEach(::add) }
            if (referencing in sm)
                add(referencing.id) {
                    sm[referencing].forEach { e ->
                        add(e) {
                            e.children("deprecated").forEach(::add)
                        }
                    }
                }
        }.also { it.sort { o1, o2 -> o1.toString().compareTo(o2.toString()) } }
    }

    override fun theme() = mapOf<Entity.Type, Style>()
}
