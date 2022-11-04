package sift.core.tree

sealed interface EntityNode {
    class Entity(
        val entity: sift.core.entity.Entity,
        override var label: String
    ) : EntityNode {
        override fun toString() = label
    }

    class Label(override var label: String) : EntityNode {
        override fun toString() = label
    }

    var label: String
}