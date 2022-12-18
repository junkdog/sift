package sift.core.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import org.objectweb.asm.Type.getInternalName
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import sift.core.element.AsmType
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.lang.IllegalStateException
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.reflect.KClass

val KClass<*>.internalName: String
    get() = java.internalName
val Class<*>.internalName: String
    get() = getInternalName(this)

fun classReader(stream: InputStream) = stream.use(::ClassReader)
fun classReader(klazz: KClass<*>)    = classReader(klazz.java)
fun classReader(file: File)          = classReader(file.inputStream().buffered())
fun classReader(bytes: ByteArray)    = ClassReader(bytes)
fun classReader(klazz: Class<*>) =
    classReader(klazz.getResourceAsStream("/${klazz.internalName}.class")!!)

inline fun <reified T> classNode()   = classNode(T::class)
fun classNode(cr: ClassReader)       = ClassNode().apply { cr.accept(this, 0) }
fun classNode(bytes: ByteArray)      = classNode(classReader(bytes))
fun classNode(klazz: KClass<*>)      = classNode(classReader(klazz))
fun classNode(klazz: Class<*>)       = classNode(classReader(klazz))
fun classNode(stream: InputStream)   = classNode(classReader(stream))
fun classNode(file: File)            = classNode(classReader(file))

/** reads all classes, where [root] points to a root directory or jar file */
fun classNodes(root: Path): List<ClassNode> = classNodes(root.toFile())
fun classNodes(root: File): List<ClassNode> = when {
    root.exists().not()       -> throw FileNotFoundException(root.path)
    root.isDirectory          -> classesDir(root)
    root.extension == "jar"   -> classesJar(root)
    root.extension == "class" -> listOf(classNode(root))
    else                      -> throw IllegalStateException(root.path)
}


val ClassNode.type: Type
    get() = Type.getType("L$name;")
val AnnotationNode.type: Type
    get() = Type.getType(desc)

inline fun <reified T> type() = type(T::class)
fun type(cls: KClass<*>) = Type.getType(cls.java)!!

val MethodInsnNode.returnType: Type
    get() = Type.getReturnType(desc)

fun MethodInsnNode.argumentTypes(): Array<Type> = Type.getArgumentTypes(desc) ?: arrayOf()
val MethodInsnNode.ownerType: Type
    get() = Type.getType("L$owner;")

val FieldInsnNode.ownerType: Type
    get() = Type.getType("L$owner;")

val Handle.ownerType: Type
    get() = Type.getType("L$owner;")

val Type.simpleName: String
    get() = simpleNameOf(this)

private fun simpleNameOf(type: Type) = when (val name = type.className) {
    "V" -> "Unit"
    "Z" -> "Boolean"
    "C" -> "Char"
    "B" -> "Byte"
    "S" -> "String"
    "I" -> "Int"
    "F" -> "Float"
    "D" -> "Double"
    "J" -> "Long"
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
    return root.walk()
        .filter { it.extension == "class" }
        .map(::classNode)
        .toList()
}