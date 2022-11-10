package sift.core.tree

import sift.core.tree.DiffNode.State

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

    fun next(): MergeOp? {
        return when {
            old.isEmpty()                       -> MergeOp(null, new.removeLastOrNull() ?: return null, State.Added)
            new.isEmpty()                       -> MergeOp(old.removeLastOrNull() ?: return null, null, State.Removed)
            nodeEquals(old.last(), new.last())  -> MergeOp(old.removeLast(), new.removeLast(), State.Unchanged)
            old.last().value > new.last().value -> MergeOp(null, new.removeLast(), State.Added)
            else                                -> MergeOp(old.removeLast(), null, State.Removed)
        }
    }

    generateSequence(::next).forEach { op ->
        when (op.state) {
            State.Unchanged -> {
                val child = node.add(DiffNode(State.Unchanged, op.new!!.value))
                merge(child, op.old!!.children(), op.new.children())
            }
            State.Added -> node.add(op.new!!.map { DiffNode(State.Added, it) })
            State.Removed -> node.add(op.old!!.map { DiffNode(State.Removed, it) })
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
    val old: Tree<EntityNode>?,
    val new: Tree<EntityNode>?,
    val state: State
)