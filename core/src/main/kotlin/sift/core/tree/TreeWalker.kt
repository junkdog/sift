package sift.core.tree

enum class TraversalType {
    DEPTH_FIRST,
    BREADTH_FIRST
}

internal class TreeWalker<T>(val root: Tree<T>, val traversal: TraversalType) : Sequence<Tree<T>> {
    override fun iterator() = object : Iterator<Tree<T>> {
        val buffer: MutableList<Tree<T>> = ArrayDeque(listOf(root))
        var nextValue: Tree<T>? = prepareNext()

        override fun hasNext() = nextValue != null

        override fun next(): Tree<T> {
            val value = nextValue ?: error("no more elements")
            nextValue = prepareNext()
            return value
        }

        private fun prepareNext(): Tree<T>? {
            return when (traversal) {
                TraversalType.DEPTH_FIRST -> buffer.removeLastOrNull()
                    ?.also { node -> buffer.addAll(node.children().reversed()) }
                TraversalType.BREADTH_FIRST -> buffer.removeFirstOrNull()
                    ?.also { node -> buffer.addAll(node.children()) }
            }
        }
    }
}