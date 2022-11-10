package sift.core.tree

sealed class EntityNode(var label: String) : Comparable<EntityNode> {
    private val properties: MutableMap<String, Any> = mutableMapOf()

    class Entity(
        val entity: sift.core.entity.Entity,
        label: String
    ) : EntityNode(label) {
        override fun toString() = label
    }

    class Label(label: String) : EntityNode(label)

    operator fun set(property: String, value: Any) {
        properties[property] = value
    }

    operator fun get(property: String): Any? = properties[property]

    override fun compareTo(other: EntityNode): Int {
        return label.compareTo(other.label)
    }

    override fun toString() = label
}

val Tree<EntityNode>.label: String
    get() = when (val v = value) {
        is EntityNode.Entity -> v.label
        is EntityNode.Label  -> v.label
    }