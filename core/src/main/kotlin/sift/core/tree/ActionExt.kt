package sift.core.tree

import sift.core.api.Action

fun Action<*, *>.debugTree():  String = toTree().toString()

fun Action<*, *>.toTree(): Tree<String> {
    // recursively build tree from action id()
    fun buildTree(action: Action<*, *>): Tree<String> {
        val tree = Tree(action.id())
        when (action) {
            is Action.Chain<*> -> {
                action.actions.map(::buildTree).forEach(tree::add)
            }
            is Action.Compose<*, *, *> -> {
                tree.add(buildTree(action.a))
                tree.add(buildTree(action.b))
            }
            is Action.Fork<*, *> -> {
                tree.add(buildTree(action.forked))
            }
            is Action.ForkOnEntityExistence<*, *> -> {
                tree.add(buildTree(action.forked))
            }
            else -> Unit
        }
        return tree
    }

    return buildTree(this)
}