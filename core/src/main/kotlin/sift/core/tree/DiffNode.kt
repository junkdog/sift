package sift.core.tree

import sift.core.pop
import sift.core.tree.DiffNode.State
import sift.core.tree.DiffNode.State.*

data class DiffNode(
    val state: State,
    val wrapped: EntityNode
) {
    enum class State {
        Unchanged, Added, Removed
    }
}

fun diffMerge(
    node: Tree<DiffNode>,
    a: List<Tree<EntityNode>>,
    b: List<Tree<EntityNode>>
) {
    val old = a.reversed().toMutableList()
    val new = b.reversed().toMutableList()

    fun next(): MergeOpLegacy? = when {
        old.isEmpty() && new.isEmpty()      -> null
        old.isEmpty()                       -> MergeOpLegacy(Added,     null,      new.pop())
        new.isEmpty()                       -> MergeOpLegacy(Removed,   old.pop(), null)
        nodeEquals(old.last(), new.last())  -> MergeOpLegacy(Unchanged, old.pop(), new.pop())
        old.last().value > new.last().value -> MergeOpLegacy(Added,     null,      new.pop())
        else                                -> MergeOpLegacy(Removed,   old.pop(), null)
    }

    generateSequence(::next).forEach { op ->
        when (op.state) {
            Added     -> node.add(op.new!!.map { DiffNode(Added, it) })
            Removed   -> node.add(op.old!!.map { DiffNode(Removed, it) })
            Unchanged -> node.add(DiffNode(Unchanged, op.new!!.value))
                .also { child -> diffMerge(child, op.old!!.children(), op.new.children()) }
        }
    }
}

private fun nodeEquals(
    a: Tree<EntityNode>?,
    b: Tree<EntityNode>?
): Boolean {
    fun type(node: Tree<EntityNode>?) = (node?.value as? EntityNode.Entity)?.entity?.type
    return a?.label == b?.label && type(a) == type(b)
}

private data class MergeOpLegacy(
    val state: State,
    val old: Tree<EntityNode>?,
    val new: Tree<EntityNode>?,
)