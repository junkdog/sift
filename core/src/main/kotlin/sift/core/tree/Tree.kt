package sift.core.tree

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.github.ajalt.mordant.rendering.TextStyle
import sift.core.terminal.Gruvbox

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
        fun print(
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
                print(n, nextIndent, isLast, out)
            }

            return out
        }

        return print(this, "", true, StringBuilder())
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

