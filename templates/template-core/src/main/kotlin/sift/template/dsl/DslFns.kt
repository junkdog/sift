package sift.template.dsl

import sift.core.api.Action
import sift.core.dsl.Dsl
import sift.core.api.Iter
import sift.core.api.IterValues
import sift.core.element.Element
import sift.core.entity.Entity
import sift.core.graphviz.Dot
import sift.core.graphviz.Shape
import sift.core.graphviz.Style
import sift.core.terminal.TextTransformer


fun Dsl.Template.graphviz(
    e: Entity.Type,
    identifyAs: Entity.Type? = null,
    rank: Int? = null,
    type: Dot? = null,
    shape: Shape? = null,
    style: Style? = null,
    arrowheadShape: String? = null,
    edgeLabel: Action<Iter<Element>, IterValues>? = null,
    label: TextTransformer
) {
    graphviz(
        e = e,
        identifyAs = identifyAs,
        rank = rank,
        type = type,
        shape = shape,
        style = style,
        arrowheadShape = arrowheadShape,
        edgeLabel = edgeLabel,
        label = arrayOf(label)
    )
}


fun Dsl.Template.graphviz(
    entities: Iterable<Entity.Type>,
    identifyAs: Entity.Type? = null,
    rank: Int? = null,
    type: Dot? = null,
    shape: Shape? = null,
    style: Style? = null,
    arrowheadShape: String? = null,
    edgeLabel: Action<Iter<Element>, IterValues>? = null,
    label: TextTransformer
) {
    graphviz(entities, identifyAs, rank, type, shape, style, arrowheadShape, edgeLabel, label = arrayOf(label))
}

fun Dsl.Template.graphviz(
    entities: Iterable<Entity.Type>,
    identifyAs: Entity.Type? = null,
    rank: Int? = null,
    type: Dot? = null,
    shape: Shape? = null,
    style: Style? = null,
    arrowheadShape: String? = null,
    edgeLabel: Action<Iter<Element>, IterValues>? = null,
    vararg label: TextTransformer
) {
    entities.forEach { e ->
        graphviz(e, identifyAs, rank, type, shape, style, arrowheadShape, edgeLabel, *label)
    }
}

/**
 * Provides graphviz configuration for [e]. All optional parameters
 * have no effect unless specified. Some parameters are mutually
 * dependent or exclusive; this function does not validate the set
 * of specified parameters.
 */
fun Dsl.Template.graphviz(
    /** Entity type to be configured. */
    e: Entity.Type,

    /** Represents an entity to be used as the identifier in place for [e]. */
    identifyAs: Entity.Type? = null,

    /**
     * The rank of [e] in the dot file. Ranks are used to specify the horizontal ordering of nodes,
     * with lower ranks appearing to the left of higher ranks.
     */
    rank: Int? = null,

    /** One of either [Dot.node] or [Dot.edge]. */
    type: Dot? = null,

    /** The shape of nodes. */
    shape: Shape? = null,

    /** The style of edges or nodes. E.g. [Style.dashed]. */
    style: Style? = null,

    /** Override shape of arrowhead, refer to https://graphviz.org/doc/info/arrows.html for values. */
    arrowheadShape: String? = null,

    /** ... */
    edgeLabel: Action<Iter<Element>, IterValues>? = null,
    vararg label: TextTransformer = arrayOf(),
) {
    elementsOf(e) {
        rank?.let {           property(e, "dot-rank", withValue(it)) }
        identifyAs?.let {     property(e, "dot-id-as", withValue(it)) }
        type?.let {           property(e, "dot-type", withValue(it)) }
        shape?.let {          property(e, "dot-shape", withValue(it.name)) }
        style?.let {          property(e, "dot-style", withValue(it.name)) }
        arrowheadShape?.let { property(e, "dot-arrowhead", withValue(it)) }
        edgeLabel?.let {      property(e, "dot-edge-label", it) }

        if (label.isNotEmpty() ) {
            property(e, "dot-label-transform", withValue(label.toList()))
        }
    }
}

@Deprecated("use Entity.Type.set() from Template scope instead")
fun Dsl.Template.registerInstantiationsOf(
    methodType: Entity.Type,
    source: Entity.Type,
    outLabel: String = "sends",
) {
    methodsOf(source) {
        source[outLabel] = methodType.instantiations
    }
}

@Deprecated("use Entity.Type.set() from Template scope instead")
fun Dsl.Template.registerInvocationsOf(
    methodType: Entity.Type,
    source: Entity.Type,
    outLabel: String = "sends",
) {
    methodsOf(source) {
        source[outLabel] = methodType.invocations
    }
}
