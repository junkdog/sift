package sift.core.entity

import sift.core.element.Element
import sift.core.Throw

class EntityService {
    private val entityToElement: MutableMap<Entity, Element> = mutableMapOf()
    private val elementToEntity: MutableMap<Element, Entity> = mutableMapOf()
    internal val entitiesByType: MutableMap<Entity.Type, MutableMap<Element, Entity>> = mutableMapOf()

    internal fun register(entity: Entity, element: Element): Entity {

        val existing = elementToEntity[element]
        return when {
            existing == null -> {
                entityToElement[entity] = element
                elementToEntity[element] = entity
                entitiesByType.getOrPut(entity.type, ::mutableMapOf)[element] = entity
                entity
            }
            existing.type == entity.type -> {
                existing.label = entity.label
                existing.properties += entity.properties
                existing
            }
            else -> Throw.entityAlreadyExists(entity, existing, element)
        }
    }

    fun allEntities(): Set<Entity> = entityToElement.keys.toSet()

    operator fun contains(element: Element): Boolean = element in elementToEntity
    operator fun contains(type: Entity.Type): Boolean = type in entitiesByType

    operator fun get(entity: Entity): Element = entityToElement[entity]!!
    operator fun get(element: Element): Entity? = elementToEntity[element]
    operator fun get(type: Entity.Type): Map<Element, Entity> = entitiesByType[type]
        ?: mapOf()
}