package sift.instrumenter.dsl

import sift.core.api.Action
import sift.core.api.Dsl
import sift.core.api.Iter
import sift.core.api.IterValues
import sift.core.element.Element
import sift.core.entity.Entity
import sift.core.graphviz.Dot
import sift.core.graphviz.Shape
import sift.core.graphviz.Style
import sift.core.tree.TreeDsl

fun Dsl.Instrumenter.graphviz(
    e: Entity.Type,
    identifyAs: Entity.Type? = null,
    rank: Int? = null,
    type: Dot? = null,
    shape: Shape? = null,
    stripLabelSuffix: String? = null,
    style: Style? = null,
    arrowheadShape: String? = null,
    edgeLabel: Action<Iter<Element>, IterValues>? = null
) {
    elementsOf(e) {
        rank?.let {             property(e, "dot-rank", withValue(it)) }
        identifyAs?.let {       property(e, "dot-id-as", withValue(it)) }
        type?.let {             property(e, "dot-type", withValue(it)) }
        shape?.let {            property(e, "dot-shape", withValue(it.name)) }
        stripLabelSuffix?.let { property(e, "dot-label-strip", withValue(it)) }
        style?.let {            property(e, "dot-style", withValue(it.name)) }
        arrowheadShape?.let {   property(e, "dot-arrowhead", withValue(it)) }
        edgeLabel?.let {        property(e, "dot-edge-label", it) }
    }
}

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
