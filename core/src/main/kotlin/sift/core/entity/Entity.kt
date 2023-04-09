package sift.core.entity

import java.util.UUID


class Entity(
    val type: Type,
    var label: String,
    values: Map<String, MutableList<Any>> = mapOf(),
    val id: UUID = UUID.randomUUID()
) {
    internal val children: MutableMap<String, MutableSet<Entity>> = mutableMapOf()
    internal val properties: MutableMap<String, MutableList<Any>> = values.toMutableMap()

    fun properties(): Map<String, List<Any>> = properties

    operator fun set(property: String, value: Any) {
        val v = if (value !is List<*>) listOf(value) else value as List<Any>
        properties.getOrPut(property, ::mutableListOf).addAll(v)
    }

    operator fun get(property: String): List<Any>? {
        return properties[property]
    }

    operator fun minusAssign(property: String) {
        properties -= property
    }

    fun remove(property: String) {
        properties -= property
    }

    override fun equals(other: Any?): Boolean {
        return (other as? Entity)?.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        val properties = listOf("type=$type") + properties
            .filterKeys { !it.startsWith("_") }
            .map { (k, v) -> k to (v.takeIf { v.size > 1 } ?: v.first()) }
            .map { (k, v) -> "$k=$v" }

        return "Entity($label, ${properties.joinToString()})"
    }

    fun addChild(label: String, child: Entity) {
        children.getOrPut(label, ::mutableSetOf) += child
    }

    internal fun addChildren(label: String, entities: Iterable<Entity>) {
        children.getOrPut(label, ::mutableSetOf) += entities
    }

    fun children(key: String): List<Entity> = (children[key] ?: listOf()).toList()
    fun children(): List<String> = children.keys.toList()

    @JvmInline
    value class Type(val id: String) {
        override fun toString() = id
    }

    interface LabelFormatter {
        fun format(entity: Entity, service: EntityService): String
    }
}