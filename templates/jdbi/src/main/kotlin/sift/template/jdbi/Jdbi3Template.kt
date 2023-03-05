package sift.template.jdbi

import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import org.objectweb.asm.Type
import sift.core.dsl.template
import sift.core.entity.Entity
import sift.core.tree.EntityNode
import sift.core.tree.Tree
import sift.core.terminal.Gruvbox.orange1
import sift.core.terminal.Gruvbox.orange2
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Style
import sift.template.spi.SystemModelTemplateServiceProvider

typealias E = Jdbi3Template.EntityTypes
typealias A = Jdbi3Template.Annotations
typealias T = Jdbi3Template.AsmTypes

@Suppress("unused")
class Jdbi3Template : SystemModelTemplate, SystemModelTemplateServiceProvider {

    override val entityTypes: Iterable<Entity.Type> = listOf(E.sqlQuery, E.sqlUpdate)
    override val defaultType: Entity.Type = entityTypes.first()

    object Annotations {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

        val sqlQuery = "org.jdbi.v3.sqlobject.statement.SqlQuery".type
        val sqlUpdate = "org.jdbi.v3.sqlobject.statement.SqlUpdate".type
    }

    object AsmTypes {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val sqlQuery = "sql-query".type
        val sqlUpdate = "sql-update".type
    }

    override fun create() = this
    override val name: String
        get() = "jdbi3"

    override fun template() = template {
        classes {
            methods {
                annotatedBy(A.sqlQuery)
                entity(
                    E.sqlQuery, label("\${sql}"),
                    property("sql", readAnnotation(A.sqlQuery, "value"))
                )
            }
            methods {
                annotatedBy(A.sqlUpdate)
                entity(
                    E.sqlUpdate, label("\${sql}"),
                    property("sql", readAnnotation(A.sqlUpdate, "value"))
                )
            }
        }
    }

    override fun theme() = mapOf(
        E.sqlQuery  to sqlStyle(),
        E.sqlUpdate to sqlStyle(),
    )
}

fun sqlStyle(
    select: TextStyle = orange1,
    insert: TextStyle = orange2,
    update: TextStyle = orange2 + bold,
    delete: TextStyle = orange2 + bold,
): Style = SqlStyle(select, insert, update, delete)

class SqlStyle(
    val select: TextStyle,
    val insert: TextStyle,
    val update: TextStyle,
    val delete: TextStyle,
) : Style {
    val repeatingWhitespace = Regex("\\s+")

    override fun format(e: Tree<EntityNode>, theme: Map<Entity.Type, Style>): String {
        val label = (e.value as EntityNode.Entity).entity.label.uppercase()
        return when {
            "DELETE FROM" in label -> delete
            "UPDATE" in label      -> update
            "INSERT INTO" in label -> insert
            else                   -> select
        }(e.value.toString().replace(repeatingWhitespace, " "))
    }
}