package sift.core.api

import org.objectweb.asm.Type
import sift.core.element.AsmType
import sift.core.element.ClassNode
import sift.core.element.Element
import sift.core.element.MethodNode
import sift.core.entity.Entity
import sift.core.jackson.NoArgConstructor

sealed class EntityAssignmentResolver<T: Element> {
    abstract val type: Entity.Type
    abstract val id: String

    internal abstract fun resolve(ctx: Context, elements: Iter<T>)

    @NoArgConstructor
    class FromInstantiationsBy(
        val key: String,
        override val type: Entity.Type,
        val instantiater: Entity.Type,
    ) : EntityAssignmentResolver<MethodNode>() {
        override val id: String = "instantiated-by"

        override fun resolve(
            ctx: Context,
            elements: Iter<MethodNode>
        ) {
            val types = ctx.entityService[type]
                .map { (elem, _) -> elem as ClassNode } // FIXME: throw
                .map(ClassNode::type)
                .toSet()

            elements
                .forEach { registerInstantiations(ctx, it, types, "backtrack", key) }
        }
    }

    @NoArgConstructor
    class FromInvocationsBy(
        val key: String,
        override val type: Entity.Type,
        val invoker: Entity.Type,
    ) : EntityAssignmentResolver<MethodNode>() {
        override val id: String = "invoked-by"

        override fun resolve(
            ctx: Context,
            elements: Iter<MethodNode> // = rhs
        ) {
            val matched: Map<MethodNode, Entity> = ctx.coercedMethodsOf(type)
            elements
                .forEach { registerInvocations(ctx, it, matched, "backtrack", key) }
        }
    }

    @NoArgConstructor
    class FromInvocationsOf(
        val key: String,
        override val type: Entity.Type
    ) : EntityAssignmentResolver<MethodNode>() {
        override val id: String = "invocations"

        override fun resolve(
            ctx: Context,
            elements: Iter<MethodNode>
        ) {
            val matched: Map<MethodNode, Entity> = ctx.coercedMethodsOf(type)
            elements
                .forEach { registerInvocations(ctx, it, matched, key, "backtrack") }
        }
    }

    @NoArgConstructor
    class FromInstantiationsOf(
        val key: String,
        override val type: Entity.Type
    ) : EntityAssignmentResolver<MethodNode>() {
        override val id: String = "instantiations"

        override fun resolve(
            ctx: Context,
            elements: Iter<MethodNode>
        ) {
            val types = ctx.entityService[type]
                .map { (elem, _) -> elem as ClassNode }
                .map(ClassNode::type)
                .toSet()

            elements
                .forEach { registerInstantiations(ctx, it, types, key, "backtrack") }
        }
    }
}

private fun instantiations(mn: MethodNode, types: Iterable<Type>): List<Type> {
    return instantiations(mn).filter(types::contains)
}

private fun registerInstantiations(
    ctx: Context,
    elem: MethodNode,
    types: Set<AsmType>,
    parentKey: String,
    childKey: String
) {
    val parent = ctx.entityService[elem]!!
    ctx.methodsInvokedBy(elem)
        .asSequence()
        .flatMap { mn -> instantiations(mn, types) }
        .distinct()
        .map { type -> ctx.classByType[type]!! }
        .mapNotNull { ctx.entityService[it] }
        .onEach { child -> parent.addChild(parentKey, child) }
        .forEach { child -> child.addChild(childKey, parent) }
}

private fun registerInvocations(
    ctx: Context,
    elem: MethodNode,
    matched: Map<MethodNode, Entity>,
    parentKey: String,
    childKey: String
) {
    val parent = ctx.entityService[elem]!!
    ctx.methodsInvokedBy(elem)
        .filter { mn -> mn in matched }
        .filter { mn -> elem != mn }
        .map { ctx.entityService[matched[it]!!] as MethodNode }
        .mapNotNull { ctx.entityService[it] }
        .onEach { child -> parent.addChild(parentKey, child) }
        .onEach { child -> child.addChild(childKey, parent) }
}