package sift.core.tree

data class DiffNode(
    val state: State,
    val wrapped: EntityNode
) {
    enum class State {
        Unchanged, Added, Removed
    }
}
