package sift.instrumenter

import com.github.ajalt.mordant.rendering.TextStyle
import sift.core.entity.Entity
import sift.core.tree.EntityNode
import sift.core.tree.Tree

private val Tree<EntityNode>.entity
    get() = (value as EntityNode.Entity).entity

interface Style {
    class Plain(val styling: TextStyle, val dedupe: Char? = null) : Style {
        override fun format(
            e: Tree<EntityNode>,
            theme: Map<Entity.Type, Style>
        ): String {
            var text = e.value.label
            if (dedupe != null) {
                val strip = Regex("$dedupe+")
                text = text.replace(strip, "$dedupe")
            }
            return styling(text)
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
                ?.let { theme[it.first() as Entity.Type] }
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

    fun format(e: Tree<EntityNode>, theme: Map<Entity.Type, Style>): String

    companion object {
        fun plain(textStyle: TextStyle, dedupe: Char? = null) = Plain(textStyle, dedupe)
        fun entityRef(key: String = "@style-as", fallback: Style) = FromEntityRef(key, fallback)
        fun fromParent() = FromParent
        fun fromProperty(key: String) = FromProperty(key)
    }
}