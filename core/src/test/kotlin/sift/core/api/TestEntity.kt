package sift.core.api

import sift.core.element.Element
import sift.core.entity.Entity

class TestEntity(
    val type: Entity.Type,
    val label: String,
    val data: Map<String, List<Any>>,
    val children: Map<String, List<TestEntity>>,
) {
    constructor(e: Entity) : this(
        e.type,
        e.label,
        e.properties,
        (e.children - "backtrack")
            .mapValues { (_, v) -> v.map { e -> TestEntity(e) } }
    )

    constructor(entry: Map.Entry<Element, Entity>) : this(entry.value)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Entity -> type == other.type
                && label == other.label
                && data == other.properties
                && children == other.children
            is TestEntity -> type == other.type
                && label == other.label
                && data == other.data
                && children == other.children
            else -> false
        }
    }

    override fun hashCode(): Int {
        var hash = type.hashCode()
        hash = hash * 31 + label.hashCode()
        hash = hash * 31 + data.hashCode()

        return hash
    }

    override fun toString(): String {
        val properties = listOf("type=$type") + data
            .filterKeys { !it.startsWith("_") }
            .map { (k, v) -> "$k=$v" }

        return "Entity($label, ${properties.joinToString()})"
    }
}

@Suppress("UNCHECKED_CAST")
fun e(
    type: Entity.Type,
    label: String,
    vararg properties: Pair<String, Any>,
    children: Map<String, List<TestEntity>> = mapOf(),
): TestEntity {
    val sanitized = properties.map { (k, v) -> k to (v as? List<Any> ?: listOf(v)) }
    return TestEntity(type, label, sanitized.toMap(), children)
}