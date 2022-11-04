package sift.core.api

import net.onedaybeard.collectionsby.filterBy
import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import sift.core.asm.asSequence
import sift.core.asm.ownerType
import sift.core.asm.type

/** extended class type, returns `null` for `java.lang.Object` */
internal val ClassNode.superType: Type?
    get() = superName
        ?.takeUnless { "java/lang/Object" in it }
        ?.let { Type.getType("L${it};") }

internal fun instantiations(mn: MethodNode): List<Type> {
    return mn.asSequence()
        .mapNotNull { it as? MethodInsnNode }
        .filter(AbstractInsnNode::isCallingConstructor)
        .map(MethodInsnNode::ownerType)
        .distinct()
        .toList()
}

internal fun methodsInvokedBy(
    mn: MethodNode,
    cns: Map<Type, ClassNode>
): Iterable<MethodNode> {
    fun recurse(current: MethodNode, known: MutableSet<MethodNode>) {
        known += current

        // java's lambda/functional interfaces
        current.asSequence()
            .mapNotNull { it as? InvokeDynamicInsnNode }
            .flatMap { ins -> ins.bsmArgs.mapNotNull { it as? Handle } }
            .mapNotNull(cns::findMethod)
            .filter { it !in known }
            .forEach { recurse(it, known) }

        // normal method invocation, kotlin lambdas
        current.asSequence()
            .mapNotNull { it as? MethodInsnNode }
            .mapNotNull(cns::findMethod)
            .filter { it !in known }
            .forEach { recurse(it, known) }
    }

    return mutableSetOf<MethodNode>()
        .apply { recurse(mn, this) }
}

internal fun interfacesOf(cn: ClassNode): List<Type> {
    return (cn.interfaces ?: listOf())
        .map { Type.getType("L${it};") }
}

internal fun Iterable<ClassNode>.parentsOf(cn: ClassNode): List<ClassNode> {
    return generateSequence(cn) { findBy(ClassNode::type, it.superType) }
        .drop(1)
        .toList()
}

internal fun Map<Type, ClassNode>.parentsOf(cn: ClassNode): List<ClassNode> {
    return generateSequence(cn) { get(it.superType) }
        .drop(1)
        .toList()
}

internal fun Map<Type, ClassNode>.findMethod(ins: MethodInsnNode): MethodNode? {
    return findMethod(ins.ownerType, ins.name, ins.desc)
}

internal fun Map<Type, ClassNode>.findMethod(handle: Handle): MethodNode? {
    return findMethod(handle.ownerType, handle.name, handle.desc)
}

internal fun Map<Type, ClassNode>.findMethod(
    owner: Type,
    name: String,
    desc: String,
): MethodNode? {
    val cn = this[owner] ?: return null

    val inherited = parentsOf(cn)
        .flatMap(ClassNode::methods)

    return (cn.methods + inherited).toSet()
        .filterBy(MethodNode::name, name)
        .filterBy(MethodNode::desc, desc)
//        .also { require(it.size < 2) { "${it.map { it.name }}" }  } // FIXME: 0..N
        .firstOrNull()
}

private fun AbstractInsnNode.isCallingConstructor(): Boolean {
    return this is MethodInsnNode && name == "<init>"
}