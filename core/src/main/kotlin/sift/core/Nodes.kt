package sift.core


internal object AsmNodeHashcoder {
    fun hash(vararg objects: Any?): Int =
        objects.fold(0) { acc, obj -> 31 * acc + obj.hashCode() }
    fun idHash(vararg objects: Any): Int =
        objects.fold(0) { acc, obj -> 31 * acc + System.identityHashCode(obj) }
}