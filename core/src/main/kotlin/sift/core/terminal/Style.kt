package sift.core.terminal

import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.*
import sift.core.entity.Entity
import sift.core.tree.DiffNode
import sift.core.tree.EntityNode
import sift.core.tree.Tree

private val Tree<EntityNode>.entity
    get() = (value as EntityNode.Entity).entity

interface Style {
    class Plain(val styling: TextStyle) : Style {
        override fun format(
            e: Tree<EntityNode>,
            theme: Map<Entity.Type, Style>
        ): String {
            return styling(e.value.label)
        }
    }

    class FromEntityRef(
        val key: String = "@style-as",
        val fallback: Style
    ) : Style {
        override fun format(
            e: Tree<EntityNode>,
            theme: Map<Entity.Type, Style>
        ): String {
            return (e.entity[key]
                ?.let { theme[it.first() as? Entity.Type] }
                ?: fallback).format(e, theme)
        }
    }

    object FromParent : Style {
        override fun format(
            e: Tree<EntityNode>,
            theme: Map<Entity.Type, Style>
        ): String {
            val parentType = (e.parent!!.value as EntityNode.Entity).entity.type
            return theme[parentType]!!.format(e, theme)
        }
    }

    class FromProperty(val typeKey: String) : Style {
        override fun format(
            e: Tree<EntityNode>,
            theme: Map<Entity.Type, Style>
        ): String {
            val type = e.entity[typeKey]?.firstOrNull() as Entity.Type?
            return when (type) {
                null -> e.value.label
                else -> theme[type]!!.format(e, theme)
            }
        }
    }

    class Diff(val wrapped: Style) : Style {
        override fun format(
            e: Tree<EntityNode>,
            theme: Map<Entity.Type, Style>
        ): String {

            val formatted = wrapped.format(e, theme)
            return when (e.value["@diff"] as? DiffNode.State) {
                null -> formatted
                DiffNode.State.Unchanged -> formatted
                DiffNode.State.Added     -> (bold + underline + italic)(formatted)
                DiffNode.State.Removed   -> strikethrough(formatted)
            }
        }
    }

    fun format(e: Tree<EntityNode>, theme: Map<Entity.Type, Style>): String

    companion object {
        fun plain(textStyle: TextStyle) = Plain(textStyle)
        fun entityRef(key: String = "@style-as", fallback: Style) = FromEntityRef(key, fallback)
        fun diff(wrap: Style) = Diff(wrap)
        fun fromParent() = FromParent
        fun fromProperty(key: String) = FromProperty(key)
    }
}