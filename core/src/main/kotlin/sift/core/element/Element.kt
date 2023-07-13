package sift.core.element

sealed abstract class Element {
    internal var id: Int = -1
    internal abstract val simpleName: String
    internal abstract val annotations: List<AnnotationNode>
}

