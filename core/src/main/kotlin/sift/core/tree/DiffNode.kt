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

fun merge(
    node: Tree<DiffNode>,
    a: List<Tree<EntityNode>>,
    b: List<Tree<EntityNode>>
) {
    val old = a.reversed().toMutableList()
    val new = b.reversed().toMutableList()

    fun next(): MergeOp? = when {
        old.isEmpty() && new.isEmpty()      -> null
        old.isEmpty()                       -> MergeOp(Added,     null,      new.pop())
        new.isEmpty()                       -> MergeOp(Removed,   old.pop(), null)
        nodeEquals(old.last(), new.last())  -> MergeOp(Unchanged, old.pop(), new.pop())
        old.last().value > new.last().value -> MergeOp(Added,     null,      new.pop())
        else                                -> MergeOp(Removed,   old.pop(), null)
    }

    generateSequence(::next).forEach { op ->
        when (op.state) {
            Added     -> node.add(op.new!!.map { DiffNode(Added, it) })
            Removed   -> node.add(op.old!!.map { DiffNode(Removed, it) })
            Unchanged -> node.add(DiffNode(Unchanged, op.new!!.value))
                .also { child -> merge(child, op.old!!.children(), op.new.children()) }
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

private data class MergeOp(
    val state: State,
    val old: Tree<EntityNode>?,
    val new: Tree<EntityNode>?,
)