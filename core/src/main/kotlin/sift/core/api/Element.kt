package sift.core.api

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode
import sift.core.asm.*
import kotlin.reflect.KClass

sealed class Element {
    data class Class(
        val cn: ClassNode,
    ) : Element() {
        override fun hashCode(): Int = AsmNodeHashcoder.hashcodeOf(this)
        override fun equals(other: Any?): Boolean {
            return other is Class
                && cn === other.cn
        }
    }

    data class Method(
        val cn: ClassNode,
        val mn: MethodNode,
    ) : Element() {
        override fun hashCode(): Int = AsmNodeHashcoder.hashcodeOf(this)
        override fun equals(other: Any?): Boolean {
            return other is Method
                && cn === other.cn
                && mn === other.mn
        }
    }

    data class Field(
        val cn: ClassNode,
        val fn: FieldNode,
    ) : Element() {
        override fun hashCode(): Int = AsmNodeHashcoder.hashcodeOf(this)
        override fun equals(other: Any?): Boolean {
            return other is Field
                && cn === other.cn
                && fn === other.fn
        }
    }

    data class Parameter(
        val cn: ClassNode,
        val mn: MethodNode,
        val pn: ParameterNode,
    ) : Element() {
        override fun hashCode(): Int = AsmNodeHashcoder.hashcodeOf(this)
        override fun equals(other: Any?): Boolean {
            return other is Parameter
                && cn === other.cn
                && mn === other.mn
                && pn === other.pn
        }
    }

    data class Value(
        val data: Any,
        val reference: Element
    ) : Element() {
        override fun hashCode(): Int = AsmNodeHashcoder.hashcodeOf(this)
        override fun equals(other: Any?): Boolean {
            return other is Value
                && reference == other.reference
                && data == other.data
        }
    }

    val simpleName: String
        get() = when (this) {
            is Class     -> cn.type.simpleName
            is Field     -> "${cn.type.simpleName}.${fn.name}"
            is Method    -> "${cn.type.simpleName}::${mn.name}"
            is Parameter -> "${cn.type.simpleName}::${mn.name}(${pn.name}: ${pn.type.simpleName})"
            is Value     -> "$data/${reference.simpleName}"
        }

    fun annotations(): Iterable<AnnotationNode> {
        return when (this) {
            is Class     -> cn.annotations()
            is Field     -> fn.annotations()
            is Method    -> mn.annotations()
            is Parameter -> pn.annotations
            is Value     -> reference.annotations()
        }
    }

    override fun toString(): String = "${this::class.simpleName}($simpleName)"
}

object AsmNodeHashcoder {
    private fun idHash(vararg objects: Any): Int =
        objects.fold(0) { acc, obj -> 31 * acc + System.identityHashCode(obj) }

    fun hashcodeOf(element: Element): Int {
        return when (val e = element) {
            is Element.Class -> idHash(e.cn)
            is Element.Field -> idHash(e.cn, e.fn)
            is Element.Method -> idHash(e.cn, e.mn)
            is Element.Parameter -> idHash(e.cn, e.mn, e.pn)
            is Element.Value -> hashcodeOf(e.reference) * 31 + e.data.hashCode()
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun Element.Method.parameters(): Iterable<ParameterNode> {
    val argumentTypes = Type.getArgumentTypes(mn.desc)

    val annotations: List<List<AnnotationNode>> by lazy {
        val visible = mn.visibleParameterAnnotations?.toList()
        val invisible = mn.invisibleParameterAnnotations?.toList()

        when {
            visible == null -> invisible?.map { it ?: listOf() }
                ?: List(argumentTypes.size) { listOf() }

            invisible == null -> visible.map { it ?: listOf() }
            else -> visible.zip(invisible) { a, b -> (a ?: listOf()) + (b ?: listOf()) }
        }
    }

    return when {
        // no parameters to resolve
        argumentTypes.isEmpty() -> listOf()

        // parameters already resolved
        mn.parameters?.firstOrNull() is ParameterNode -> mn.parameters

        // convert ASM parameter nodes to sift's
        mn.parameters?.firstOrNull() is AsmParameterNode -> argumentTypes
            .zip(annotations)
            .mapIndexed { idx, (type, anno) -> ParameterNode(mn.parameters[idx], type, anno) }
            .also { mn.parameters = it.toMutableList() as List<AsmParameterNode> }

        // create parameters from localvars
        mn.localVariables?.isNotEmpty() == true -> mn.localVariables
            .sortedBy(LocalVariableNode::index)
            .drop(1) // FIXME: 'this' assumed at index 0
            .take(argumentTypes.size)
            .zip(annotations)
            .mapIndexed { idx, (localVar, anno) -> ParameterNode(localVar, argumentTypes[idx], anno) }
            .also { mn.parameters = it.toMutableList() as List<AsmParameterNode> }

        // fallback: create parameters with names from type
        else -> argumentTypes
            .zip(annotations)
            .mapIndexed { idx, (type, ans) -> ParameterNode(idx, type, ans) }
            .also { mn.parameters = it.toMutableList() as List<AsmParameterNode> }
    } as Iterable<ParameterNode>
}

inline fun <reified T : Element> Element.into() = into(T::class)

fun <T : Element> Element.into(element: KClass<T>): T {
    return when (this) {
        is Element.Class -> when (element) {
            Element.Class::class -> this
            else -> error("can't convert ${this::class.simpleName} into ${element.simpleName}")
        }

        is Element.Field -> when (element) {
            Element.Class::class -> Element.Class(cn)
            Element.Field::class -> this
            else -> error("can't convert ${this::class.simpleName} into ${element.simpleName}")
        }

        is Element.Method -> when (element) {
            Element.Class::class  -> Element.Class(cn)
            Element.Method::class -> this
            else -> error("can't convert ${this::class.simpleName} into ${element.simpleName}")
        }

        is Element.Parameter -> when (element) {
            Element.Class::class     -> Element.Class(cn)
            Element.Method::class    -> Element.Method(cn, mn)
            Element.Parameter::class -> this
            else -> error("can't convert ${this::class.simpleName} into ${element.simpleName}")
        }
        is Element.Value -> this.reference.into(element)
    } as T
}
