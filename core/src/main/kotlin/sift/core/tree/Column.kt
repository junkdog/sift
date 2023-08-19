package sift.core.tree

import com.github.ajalt.mordant.rendering.TextStyle
import sift.core.terminal.Gruvbox.aqua1
import sift.core.terminal.Gruvbox.dark2
import sift.core.terminal.Gruvbox.dark3
import sift.core.terminal.Gruvbox.fg
import sift.core.tree.Align.LEFT
import sift.core.tree.Align.RIGHT

enum class Column(
    val property: String,
    val style: TextStyle,
    val transform: (String) -> String = { it }
) {
    All("all",                  fg), // special case; replaced by list of all other enum values
    ElementId("element-id",     dark3),
    ElementType("element-type", dark2, { s -> s.replace("Node", "").lowercase() }),
    EntityType("entity-type",   aqua1);

    fun format(node: EntityNode): String = transform(node(property))
}

private operator fun EntityNode.invoke(property: String): String = this[property]?.toString() ?: ""

enum class Align {
    LEFT, RIGHT
}

internal val Column.alignment: Align
    get() = when (this) {
        Column.All -> error("should have been replaced")
        Column.ElementId -> RIGHT
        Column.EntityType -> LEFT
        Column.ElementType -> LEFT
    }