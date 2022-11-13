package sift.core.api

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

    override fun equals(rhs: Any?): Boolean {
        return when (rhs) {
            is Entity -> type == rhs.type
                && label == rhs.label
                && data == rhs.properties
                && children == rhs.children
            is TestEntity -> type == rhs.type
                && label == rhs.label
                && data == rhs.data
                && children == rhs.children
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

fun e(
    type: Entity.Type,
    label: String,
    vararg properties: Pair<String, Any>,
    children: Map<String, List<TestEntity>> = mapOf(),
): TestEntity {
    val sanitized = properties.map { (k, v) -> k to (v as? List<Any> ?: listOf(v)) }
    return TestEntity(type, label, sanitized.toMap(), children)
}