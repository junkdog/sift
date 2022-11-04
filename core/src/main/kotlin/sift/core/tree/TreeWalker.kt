package sift.core.tree

class TreeWalker<T>(val root: Tree<T>) : Sequence<Tree<T>> {
    override fun iterator() = object : Iterator<Tree<T>> {
        val stack: MutableList<Tree<T>> = mutableListOf(root)
        var nextValue: Tree<T>? = prepareNext()

        override fun hasNext() = nextValue != null

        override fun next(): Tree<T> {
            val value = nextValue ?: error("no more elements")
            nextValue = prepareNext()
            return value
        }

        private fun prepareNext(): Tree<T>? {
            return stack.removeLastOrNull()
                ?.also { stack.addAll(it.children()) }
        }
    }
}