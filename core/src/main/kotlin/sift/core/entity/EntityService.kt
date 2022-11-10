package sift.core.entity

import sift.core.Throw
import sift.core.api.Element

class EntityService {
    private val entityToElement: MutableMap<Entity, Element> = mutableMapOf()
    private val elementToEntity: MutableMap<Element, Entity> = mutableMapOf()
    internal val entitiesByType: MutableMap<Entity.Type, MutableMap<Element, Entity>> = mutableMapOf()

    fun register(entity: Entity, element: Element) {

        val old = elementToEntity[element]
        when {
            old == null -> {
                entityToElement[entity] = element
                elementToEntity[element] = entity
                entitiesByType.getOrPut(entity.type, ::mutableMapOf)[element] = entity
            }
            old.type == entity.type -> {
                old.properties += entity.properties
            }
            else -> Throw.entityAlreadyExists(entity, old, element)
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