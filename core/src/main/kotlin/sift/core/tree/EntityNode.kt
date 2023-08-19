package sift.core.tree

sealed class EntityNode(var label: String) : Comparable<EntityNode> {
    private val properties: MutableMap<String, Any> = mutableMapOf()

    class Entity(
        val entity: sift.core.entity.Entity,
        label: String
    ) : EntityNode(label) {
        override fun toString() = label

        override fun hashCode(): Int = entity.id.hashCode()
        override fun equals(other: Any?): Boolean {
            return (other as? Entity)?.entity?.id == entity.id
        }
    }

    class Label(label: String) : EntityNode(label) {
        override fun hashCode(): Int = label.hashCode()
        override fun equals(other: Any?): Boolean {
            return (other as? Label)?.label == label
        }
    }

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