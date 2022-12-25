package sift.core

import sift.core.api.Action
import sift.core.element.Element
import sift.core.entity.Entity
import java.lang.RuntimeException


object Throw {
    fun entityAlreadyExists(new: Entity, old: Entity, element: Element): Nothing {
        throw "unable to associate '${new.type}' type to ${element.simpleName} as it is already registered to '${old.type}'"
            .let(::UniqueElementPerEntityViolation)
    }

    fun publishOutsideOfProperty(action: Action<*, *>): Nothing {
        throw "cannot publish($action) outside of entity property scopes"
            .let(::PublishViolationException)
    }

    fun publishNeverCalled(tag: String): Nothing {
        throw "missing publish() for $tag"
            .let(::PublishViolationException)
    }

    fun entityNotFound(type: Entity.Type, element: Element): Nothing {
        throw "entity.type='$type' not associated with ${element.simpleName}"
            .let(::EntityNotFoundException)
    }

    fun entityNotFound(element: Element): Nothing {
        throw "No entity associated with ${element.simpleName}"
            .let(::EntityNotFoundException)
    }

    fun entityNotRegistered(type: Entity.Type): Nothing {
        throw "'$type' is not a registered entity"
            .let(::EntityNotFoundException)
    }

    fun unableToResolveParentRelation(parentType: Entity.Type, childType: Entity.Type): Nothing {
        throw "Unable to establish relation between '$parentType' and '$childType'"
            .let(::FailedToResolveParentException)
    }
}

sealed class SiftException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

sealed class SiftModelException(
    message: String,
    cause: Throwable? = null
) : SiftException(message, cause)

internal class UniqueElementPerEntityViolation(
    message: String
) : SiftModelException(message)

internal class PublishViolationException(
    message: String
) : SiftModelException(message)

internal open class EntityNotFoundException(
    message: String
) : SiftModelException(message)

internal open class EntityRelationException(
    message: String
) : SiftModelException(message)

internal class FailedToResolveParentException(
    message: String
) : EntityNotFoundException(message)

internal class IllegalEntityAssignmentException(
    message: String
) : SiftModelException(message)

internal class RhsEntityResolutionException(
    message: String
) : SiftModelException(message)