package sift.template.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.api.IterValues
import sift.core.dsl.Elements
import sift.core.dsl.Template
import sift.core.element.Element
import sift.core.entity.Entity
import sift.core.graphviz.Dot
import sift.core.graphviz.Shape
import sift.core.graphviz.Style
import sift.core.terminal.TextTransformer


fun Template.graphviz(
    entities: Iterable<Entity.Type>,
    identifyAs: Entity.Type? = null,
    rank: Int? = null,
    type: Dot? = null,
    shape: Shape? = null,
    style: Style? = null,
    arrowheadShape: String? = null,
    edgeLabel: (Elements.() -> Action<Iter<Element>, IterValues>)? = null,
    label: TextTransformer? = null,
) {
    entities.forEach { e ->
        graphviz(e, identifyAs, rank, type, shape, style, arrowheadShape, edgeLabel, label)
    }
}

/**
 * Provides graphviz configuration for [e]. All optional parameters
 * have no effect unless specified. Some parameters are mutually
 * dependent or exclusive; this function does not validate the set
 * of specified parameters.
 *
 * @param e the entity to configure
 * @param identifyAs represents an entity to be used as the identifier in place for [e].
 * @parm rank  the rank of [e] in the dot file. Ranks are used to specify the horizontal ordering of nodes, with lower ranks appearing to the left of higher ranks.
 * @param type one of either [Dot.node] or [Dot.edge].
 * @param shape the shape of the node.
 * @param style the style of edges or nodes. E.g. [Style.dashed].
 * @param arrowheadShape the shape of the arrowhead. Refer to https://graphviz.org/doc/info/arrows.html for values.
 * @param edgeLabel used in conjunction with [identifyAs].
 * @param label applies [transformations][TextTransformer] to the label of [e].
 */
fun Template.graphviz(
    e: Entity.Type,
    identifyAs: Entity.Type? = null,
    rank: Int? = null,
    type: Dot? = null,
    shape: Shape? = null,
    style: Style? = null,
    arrowheadShape: String? = null,
    edgeLabel: (Elements.() -> Action<Iter<Element>, IterValues>)? = null,
    label: TextTransformer? = null,
) {
    elementsOf(e) {
        rank?.let {           property(e, "dot-rank", withValue(it)) }
        identifyAs?.let {     property(e, "dot-id-as", withValue(it)) }
        type?.let {           property(e, "dot-type", withValue(it)) }
        shape?.let {          property(e, "dot-shape", withValue(it.name)) }
        style?.let {          property(e, "dot-style", withValue(it.name)) }
        arrowheadShape?.let { property(e, "dot-arrowhead", withValue(it)) }
        edgeLabel?.let {      property(e, "dot-edge-label",      it()) }
        label?.let {          property(e, "dot-label-transform", editor(it)) }
    }
}
