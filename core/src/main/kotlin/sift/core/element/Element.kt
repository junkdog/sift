package sift.core.element

sealed class Element : Comparable<Element> {
    internal var id: Int = -1
    internal abstract val simpleName: String
    internal abstract val annotations: List<AnnotationNode>

    override fun compareTo(other: Element): Int {
        return id.compareTo(other.id)
    }
}

