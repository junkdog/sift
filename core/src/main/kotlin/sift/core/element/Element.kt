package sift.core.element

sealed interface Element {
    val simpleName: String
    val annotations: List<AnnotationNode>
}

