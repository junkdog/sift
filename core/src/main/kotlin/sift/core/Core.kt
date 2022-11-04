package sift.core

import net.onedaybeard.collectionsby.filterBy
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor
import org.objectweb.asm.util.TraceMethodVisitor
import sift.core.asm.classNode
import sift.core.collections.MultiIterable
import sift.core.collections.MutableMultiIterable
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.IllegalStateException
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

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

///** reads all classes, where [root] points to a root directory or jar file */
//fun classNodes(root: Path): List<ClassNode> = when {
//    root.exists().not()     -> throw FileNotFoundException(root.toString())
//    root.isDirectory()      -> classesDir(root.toFile())
//    root.extension == "jar" -> classesJar(root.toFile())
//    else                    -> throw IllegalStateException(root.toString())
//}
//
//private fun classesJar(root: File): List<ClassNode> {
//    return ZipFile(root).use { archive ->
//        archive.entries()
//            .asSequence()
//            .filterBy(ZipEntry::getName) { it.endsWith(".class") }
//            .map(archive::getInputStream)
//            .map(::classNode)
//            .toList()
//    }
//}
//
//private fun classesDir(root: File): List<ClassNode> {
//    return root.walk()
//        .filterBy(File::extension, "class")
//        .map(::classNode)
//        .toList()
//}

fun stringWriter(f: StringWriter.() -> Unit): String = StringWriter().apply(f).toString()

/** ensures [f] is only executed once, returning the original result on subsequent invocations */
fun <T, U> memoize1(f: (T) -> U) = object : (T) -> U {
    private var results: MutableMap<T, U> = mutableMapOf()

    override operator fun invoke(t: T): U = results.getOrPut(t) { f(t) }
}

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

internal class SynthesisTemplate