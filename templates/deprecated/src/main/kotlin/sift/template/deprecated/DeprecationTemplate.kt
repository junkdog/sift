package sift.template.deprecated

import sift.core.entity.Entity
import sift.core.api.Action
import sift.core.api.SystemModel
import sift.core.dsl.Fields
import sift.core.dsl.Methods
import sift.core.dsl.template
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.tree.TreeDsl.Companion.tree
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style
import sift.template.spi.SystemModelTemplateServiceProvider

typealias JavaDeprecated = java.lang.Deprecated

@Suppress("unused")
class DeprecationTemplate : SystemModelTemplate, SystemModelTemplateServiceProvider {
    val klazz = Entity.Type("classes")
    val referencing = Entity.Type("classes with deprecations")
    val method = Entity.Type("methods")
    val field = Entity.Type("fields")

    override val entityTypes: Iterable<Entity.Type> = listOf(klazz, method, field, referencing)
    override val defaultType: Entity.Type = entityTypes.first()

    override fun create() = this
    override val name: String
        get() = "deprecations"

    override fun template(): Action<Unit, Unit> {
        fun Methods.registerWithParent() {
            outerScope("tag class") { entity(referencing) }
            referencing["deprecated"] = method
        }

        fun Fields.registerWithParent() {
            outerScope("tag class") { entity(referencing) }
            referencing["deprecated"] = field
        }

        return template {
            classes {
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
