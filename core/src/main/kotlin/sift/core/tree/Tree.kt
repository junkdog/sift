package sift.core.tree

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.github.ajalt.mordant.rendering.TextStyle
import sift.core.pop
import sift.core.terminal.Gruvbox
import sift.core.terminal.Gruvbox.light2
import sift.core.tree.MergeOrigin.*

@JsonIdentityInfo(scope = Tree::class, generator = ObjectIdGenerators.IntSequenceGenerator::class)
@JsonIgnoreProperties("depth", "prev", "next")
class Tree<T>(val value: T) {
    var parent: Tree<T>? = null
        private set

    private val nodes: MutableList<Tree<T>> = mutableListOf()

    val depth: Int
        get() = parents().count()

    var index: Int = 0
        private set

    val next: Tree<T>?
        get() = when (parent?.nodes?.size) {
            null -> null
            index -> null
            else -> parent!!.nodes[index + 1]
        }

    val prev: Tree<T>?
        get() = when (index) {
            0    -> parent
            else -> parent!!.nodes[index - 1]
        }

    fun delete() {
        parent?.let {
            nodes.remove(this)
            nodes.forEachIndexed { index, node -> node.index = index }
        }
        parent?.nodes?.remove(this)
        parent = null
    }

    fun add(node: Tree<T>): Tree<T> {
        node.delete() // clear any previous association
        node.parent = this
        node.index = nodes.size
        nodes += node

        return node
    }

    fun add(value: T): Tree<T> {
        return add(Tree(value))
    }

    fun children(): List<Tree<T>> = nodes.toList()

    override fun equals(other: Any?) = (other as? Tree<*>)?.value == value
    override fun hashCode() = value.hashCode()

    fun walk(): TreeWalker<T> = TreeWalker(this)

    fun copy(): Tree<T> = map { it }

    fun <U> map(f: (T) -> U): Tree<U> {
        return Tree(f(value)).also { tree ->
            children().map { it.map(f) }.forEach(tree::add)
        }
    }

    override fun toString(): String {
        return toString({ it.toString() })
    }

    fun toString(
        format: (T) -> String,
        prefix: (T) -> String = { "" },
        structure: TextStyle = Gruvbox.light2
    ): String {
        fun render(
            node: Tree<T>,
            indent: String,
            last: Boolean,
            out: StringBuilder
        ): StringBuilder {
            val delim = if (last) '└' else '├'
            val value = format(node.value)
            out.append("${prefix(node.value)}${structure("$indent$delim─")} ${value}\n")

            val nextIndent = indent + (if (last) "   " else "│  ")
            node.nodes.forEachIndexed { i, n ->
                val isLast = i == node.nodes.lastIndex
                render(n, nextIndent, isLast, out)
            }

            return out
        }

        return render(this, "", true, StringBuilder())
            .also { it[it.indexOf('└')] = '─' }
            .toString()
    }

    fun sort(comparator: Comparator<in T>) {
        walk()
            .filter { it.nodes.isNotEmpty() }
            .forEach { it.nodes.sortWith { o1, o2 -> comparator.compare(o1.value, o2.value) } }
    }

    fun parents(): List<Tree<T>> = when (parent) {
        null -> listOf()
        else -> generateSequence(parent, Tree<T>::parent).toList()
    }
}

fun <T: Any, S: Comparable<S>> merge(
    a: Tree<S>,
    b: Tree<S>,
    nodeEquals: (Tree<S>, Tree<S>) -> Boolean = { l, r -> l.value == r.value },
    transform: (S, MergeOrigin) -> T = { s, _ -> s as T }
): Tree<T> = Tree(transform(a.value, both)).apply {
    require(a.value == b.value) { "trees must share the same root node" }
    merge(a.children(), b.children(), nodeEquals, transform)
}

fun <T: Any, S: Comparable<S>> merge(
    root: T,
    a: Tree<S>,
    b: Tree<S>,
    nodeEquals: (Tree<S>, Tree<S>) -> Boolean = { l, r -> l.value == r.value },
    transform: (S, MergeOrigin) -> T = { s, _ -> s as T }
): Tree<T> = Tree(root).apply {
    val lhs = if (transform(a.value, both) == root) a.children() else listOf(a)
    val rhs = if (transform(b.value, both) == root) b.children() else listOf(b)
    merge(lhs, rhs, nodeEquals, transform)
}

private fun <T, S : Comparable<S>> Tree<T>.merge(
    a: List<Tree<S>>,
    b: List<Tree<S>>,
    nodeEquals: (Tree<S>, Tree<S>) -> Boolean = { l, r -> l.value == r.value },
    transform: (S, MergeOrigin) -> T
) {
    val lhs = a.reversed().toMutableList()
    val rhs = b.reversed().toMutableList()

    fun next(): MergeOp<S>? = when {
        lhs.isEmpty() && rhs.isEmpty()      -> null
        lhs.isEmpty()                       -> MergeOp(right, null,      rhs.pop())
        rhs.isEmpty()                       -> MergeOp(left,  lhs.pop(), null)
        nodeEquals(lhs.last(), rhs.last())  -> MergeOp(both,  lhs.pop(), rhs.pop())
        lhs.last().value > rhs.last().value -> MergeOp(right, null,      rhs.pop())
        else                                -> MergeOp(left,  lhs.pop(), null)
    }

    generateSequence(::next).forEach { op ->
        when (op.state) {
            left  -> add(op.lhs!!.map { transform(it, left) })
            right -> add(op.rhs!!.map { transform(it, right) })
            both  -> add(transform(op.lhs!!.value, both))
                .also { child -> child.merge(op.lhs.children(), op.rhs!!.children(), nodeEquals, transform) }
        }
    }
}

private data class MergeOp<S : Comparable<S>>(
    val state: MergeOrigin,
    val lhs: Tree<S>?,
    val rhs: Tree<S>?,
)

@Suppress("EnumEntryName")
enum class MergeOrigin {
    left, right, both
}

fun Tree<EntityNode>.tabulate(
    format: (EntityNode) -> String,
    columns: List<Column>,
): String {
    val table = Table(columns)

    fun render(
        node: Tree<EntityNode>,
        indent: String,
        last: Boolean,
    ) {
        val row = mutableListOf<String>()

        columns.map { col -> col.format(node.value) }
            .forEach { c -> row += c }

        val delim = if (last) '└' else '├'
        row += "${light2("$indent$delim─")} ${format(node.value)}"
        table += row

        val nextIndent = indent + (if (last) "   " else "│  ")
        node.children().forEachIndexed { i, n ->
            val isLast = i == node.children().lastIndex
            render(n, nextIndent, isLast)
        }
    }

    render(this@tabulate, "", true)

    return table.render()
}

private class Table(
    val columns: List<Column>,
) {
    private val rows: MutableList<List<String>> = mutableListOf()
    private val colWidths = IntArray(columns.size)

    fun addRow(row: List<String>) {
        colWidths.forEachIndexed { index, i -> colWidths[index] = maxOf(i, row[index].length) }
        rows += row
    }

    fun render(): String {

        fun renderRow(row: List<String>): String = row
            .dropLast(1)
            .mapIndexed { index, s -> columns[index].stylize(s, colWidths[index]) }
            .joinToString(" ") + " ${row.last()}"

        return rows.joinToString("\n") { row -> renderRow(row) }
    }

    operator fun plusAssign(row: List<String>) = addRow(row)

    fun Column.stylize(s: String, width: Int): String {
        return style(when (alignment) {
            Align.LEFT  -> s.padEnd(width)
            Align.RIGHT -> s.padStart(width)
        })
    }
}