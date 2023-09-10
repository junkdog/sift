package sift.core.asm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Type.getInternalName
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import sift.core.dsl.Type
import sift.core.element.AsmType
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.reflect.KClass

val KClass<*>.internalName: String
    get() = java.internalName
val Class<*>.internalName: String
    get() = getInternalName(this)

internal fun classReader(stream: InputStream) = stream.use(::ClassReader)
internal fun classReader(klazz: KClass<*>)    = classReader(klazz.java)
internal fun classReader(file: File)          = classReader(file.inputStream().buffered())
internal fun classReader(bytes: ByteArray)    = ClassReader(bytes)
internal fun classReader(klazz: Class<*>) =
    classReader(klazz.getResourceAsStream("/${klazz.internalName}.class")!!)

internal inline fun <reified T> classNode()   = classNode(T::class)
internal fun classNode(cr: ClassReader)       = ClassNode().apply { cr.accept(this, 0) }
internal fun classNode(bytes: ByteArray)      = classNode(classReader(bytes))
internal fun classNode(klazz: KClass<*>)      = classNode(classReader(klazz))
internal fun classNode(klazz: Class<*>)       = classNode(classReader(klazz))
internal fun classNode(stream: InputStream)   = classNode(classReader(stream))
internal fun classNode(file: File)            = classNode(classReader(file))

/** reads all classes, where [root] points to a root directory or jar file */
internal fun classNodes(root: Path): List<ClassNode> = classNodes(root.toFile())
internal fun classNodes(root: File): List<ClassNode> = when {
    root.exists().not()       -> throw FileNotFoundException(root.path)
    root.isDirectory          -> classesDir(root)
    root.extension == "jar"   -> classesJar(root)
    root.extension == "class" -> listOf(classNode(root))
    else                      -> throw IllegalStateException(root.path)
}


val ClassNode.type: Type
    get() = Type.from(name)
val AnnotationNode.type: Type
    get() = Type.fromTypeDescriptor(desc)

val MethodInsnNode.ownerType: Type
    get() = Type.from(owner)

val FieldInsnNode.ownerType: Type
    get() = Type.from(owner)

val Handle.ownerType: Type
    get() = Type.from(owner)

val AsmType.simpleName: String
    get() = simpleNameOf(this)

private fun simpleNameOf(type: AsmType) = when (val name = type.className) {
    "V" -> "void"
    "Z" -> "boolean"
    "C" -> "char"
    "B" -> "byte"
    "S" -> "String"
    "I" -> "int"
    "F" -> "float"
    "D" -> "double"
    "J" -> "long"
    else -> name
        .substringAfterLast(".")
        .replace("$", ".")
}

private fun classesJar(root: File): List<ClassNode> {
    return ZipFile(root).use { archive ->
        archive.entries()
            .asSequence()
            .filter { it.name.endsWith(".class") }
            .map(archive::getInputStream)
            .map(::classNode)
            .toList()
    }
}

private fun classesDir(root: File): List<ClassNode> {
    return runBlocking(Dispatchers.IO) {
        root.walk()
            .filter { it.extension == "class" }
            .map(File::readBytes)
            .asFlow()
            .map(::classNode)
            .toList()
    }
}