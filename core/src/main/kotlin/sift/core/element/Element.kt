package sift.core.element

sealed interface Element {
    var id: Int
    val simpleName: String
    val annotations: List<AnnotationNode>
}

