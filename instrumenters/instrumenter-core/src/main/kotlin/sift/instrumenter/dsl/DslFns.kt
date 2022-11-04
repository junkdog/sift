package sift.instrumenter.dsl

import sift.core.api.Dsl
import sift.core.entity.Entity
import sift.core.tree.TreeDsl

fun Dsl.Instrumenter.registerInstantiationsOf(
    methodType: Entity.Type,
    source: Entity.Type,
    outLabel: String = "sends",
) {
    methodsOf(source) {
        source[outLabel] = methodType.instantiations
    }
}

fun Dsl.Instrumenter.registerInvocationsOf(
    methodType: Entity.Type,
    source: Entity.Type,
    outLabel: String = "sends",
) {
    methodsOf(source) {
        source[outLabel] = methodType.invocations
    }
}

fun TreeDsl.buildTree(
    e: Entity,
    vararg exceptChildren: String = arrayOf("sent-by", "backtrack")
) {
    (e.children() - exceptChildren.toSet()).forEach { key ->
        e.children(key).forEach { child: Entity ->
            // FIXME: selfReferential is a bug in establishing relations
            if (!selfReferential(child)) {
                add(child) { buildTree(child) }
            }
        }
    }
}
