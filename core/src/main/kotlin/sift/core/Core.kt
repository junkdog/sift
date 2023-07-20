package sift.core

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor
import org.objectweb.asm.util.TraceMethodVisitor
import sift.core.collections.MultiIterable
import sift.core.collections.MutableMultiIterable
import java.io.PrintWriter
import java.io.StringWriter

fun <T> anyOf(vararg predicates: (T) -> Boolean): (T) -> Boolean = { t -> predicates.any { it(t) } }
fun <T> allOf(vararg predicates: (T) -> Boolean): (T) -> Boolean = { t -> predicates.all { it(t) } }
fun <T> noneOf(vararg predicates: (T) -> Boolean): (T) -> Boolean = { t -> predicates.none { it(t) } }

/** Combines multiple nullable [MutableIterable] into one */
@JvmName("combineMutable")
fun <T> combine(vararg iterables: MutableIterable<T>?): MutableIterable<T>
    = MutableMultiIterable(iterables.toList())

/** Combines multiple nullable [Iterable] into one */
fun <T> combine(vararg iterables: Iterable<T>?): Iterable<T>
    = MultiIterable(iterables.toList())

fun ClassNode.toDebugString(): String = stringWriter {
    accept(TraceClassVisitor(PrintWriter(this)))
}

fun MethodNode.toDebugString(): String = stringWriter {
    val printer = Textifier()
    accept(TraceMethodVisitor(printer))
    printer.print(PrintWriter(this))
}

fun <T> MutableList<T>.pop() = removeLast()
fun <T> MutableList<T>.push(t: T) = add(t)

fun stringWriter(f: StringWriter.() -> Unit): String = StringWriter().apply(f).toString()

/** ensures [f] is only executed once, returning the original result on subsequent invocations */
fun <T> memoize(f: () -> T) = object : () -> T {
    private var result: T? = null

    override operator fun invoke(): T {
        if (result == null)
            result = f()

        return result!!
    }
}

fun <T, U> Iterable<T>.product(
    rhs: Iterable<U>
): List<Pair<T, U>> = flatMap { l -> rhs.map { r -> l to r } }

fun <T> Iterable<T>.topologicalSort(parentsOf: (T) -> List<T>): List<T> {
    val edges = associate { it to parentsOf(it) }

    val unvisited = toMutableSet()
    val visiting = mutableSetOf<T>()
    val visited = mutableSetOf<T>()

    val sortedNodes = mutableListOf<T>()

    fun visit(node: T) {
        when {
            !unvisited.remove(node) -> return
            !visiting.add(node)     -> error("cycle detected: $node") // return ?
            else                    -> edges[node]?.forEach { visit(it) }
        }
        visiting.remove(node)
        visited.add(node)
        sortedNodes += node
    }

    while (unvisited.isNotEmpty()) {
        visit(unvisited.iterator().next())
    }

    return sortedNodes
}

internal class SynthesisTemplate