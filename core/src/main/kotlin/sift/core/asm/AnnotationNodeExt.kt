package sift.core.asm

import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.tree.AnnotationNode
import sift.core.dsl.Type
import kotlin.reflect.KProperty1

operator fun Iterable<AnnotationNode>?.contains(type: Type) =
    this?.findAnnotation(type) != null

internal fun Iterable<AnnotationNode>.asTypes(): List<Type> = map(AnnotationNode::type)

internal fun Iterable<AnnotationNode>.findAnnotation(type: Type) =
    findBy(AnnotationNode::desc, type.descriptor)

internal inline fun <reified T : Annotation> Iterable<AnnotationNode>.findAnnotation() =
    findAnnotation(Type.from(T::class))

/** Reads value from annotation property where [R] is a primitive value or string */
internal inline fun <reified T : Annotation, reified R> Iterable<AnnotationNode>.read(
    field: KProperty1<T, R>
) = findAnnotation<T>()?.let { readAttribute<R>(field.name) }

/** Reads class value of annotation property as type */
internal inline fun <reified T : Annotation> Iterable<AnnotationNode>.readType(
    field: KProperty1<T, *>
) = findAnnotation<T>()?.let { readAttribute<Type>(field.name) }

/** Reads class values of annotation property as types */
internal inline fun <reified T : Annotation> Iterable<AnnotationNode>.readTypes(
    field: KProperty1<T, Array<*>>
) = findAnnotation<T>()?.let { readAttribute<List<Type>>(field.name) }

internal inline fun <reified T> readAttribute(
    name: String
): (AnnotationNode) -> T? = { an ->
    val values = an.values ?: listOf()
    val index = values
        .filterIndexed { i, _ -> i % 2 == 0 }
        .indexOf(name)

    when (val any = values.getOrNull(index * 2 + 1)) {
        is T -> any
        null -> null
        else -> error("${any::class.simpleName} is not of type ${T::class.simpleName}")
    }
}

internal fun readAttributeAny(
    name: String
): (AnnotationNode) -> Any? = { an ->
    val values = an.values ?: listOf()
    val index = values
        .filterIndexed { i, _ -> i % 2 == 0 }
        .indexOf(name)

    when (val any = values.getOrNull(index * 2 + 1)) {
        null -> null
        else -> any
    }
}