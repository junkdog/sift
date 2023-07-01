package sift.core

import sift.core.api.Measurement
import sift.core.element.Element
import sift.core.entity.Entity
import sift.core.tree.Tree
import kotlin.reflect.KClass

internal object Throw {
    fun illegalGenericCast(from: KClass<*>, to: KClass<*>): Nothing {
        throw IllegalGenericCastException(from, to)
    }

    fun entityAlreadyExists(new: Entity, old: Entity, element: Element): Nothing {
        throw "unable to associate '${new.type}' type to ${element.simpleName} as it is already registered to '${old.type}'"
            .let(::UniqueElementPerEntityViolation)
    }

    fun entityNotFound(type: Entity.Type, element: Element): Nothing {
        throw "entity.type='$type' not associated with ${element.simpleName}"
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
    
    fun entityTypeAlreadyBoundToElementType(
        type: Entity.Type,
        registered: KClass<out Element>,
        requested: KClass<out Element>
    ): Nothing {
        val a = registered.simpleName
        val b = requested.simpleName
        throw "Entity Type '$type' is already associated with $a elements, it cannot be registered to $b at the same time."
            .let(::IllegalEntityAssignmentException)
    }
}

internal sealed class SiftException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

internal sealed class SiftModelException(
    message: String,
    cause: Throwable? = null
) : SiftException(message, cause)

@Suppress("CanBeParameter")
internal class UnexpectedElementException(
    val expected: KClass<out Element>,
    val actual: KClass<out Element>,
) : SiftModelException("Expected class ${expected.simpleName}, got class ${actual.simpleName}")

internal class UniqueElementPerEntityViolation(
    message: String
) : SiftModelException(message)

internal open class EntityNotFoundException(
    message: String
) : SiftModelException(message)

internal class FailedToResolveParentException(
    message: String
) : EntityNotFoundException(message)

internal class IllegalEntityAssignmentException(
    message: String
) : SiftModelException(message)

internal class IllegalGenericCastException(
    val from: KClass<*>,
    val to: KClass<*>,
) : SiftException("Cannot cast collection of ${from.simpleName} to collection of ${to.simpleName}")

internal class TemplateProcessingException(
    val trace: Tree<Measurement>?,
    override val cause: Throwable,
) : SiftException("Template threw exception: ${cause.message}", cause)