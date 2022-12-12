package sift.core


object AsmNodeHashcoder {
    fun hash(vararg objects: Any): Int =
        objects.fold(0) { acc, obj -> 31 * acc + obj.hashCode() }
    fun idHash(vararg objects: Any): Int =
        objects.fold(0) { acc, obj -> 31 * acc + System.identityHashCode(obj) }

//    fun hashcodeOf(element: Element): Int {
//        return when (val e = element) {
//            is Element.Class     -> idHash(e.cn)
//            is Element.Field     -> idHash(e.cn, e.fn)
//            is Element.Method    -> idHash(e.cn, e.mn)
//            is Element.Parameter -> idHash(e.cn, e.mn, e.pn)
//            is Element.Signature -> hashcodeOf(e.reference) * 31 + e.signature.hashCode()
//            is Element.Value     -> hashcodeOf(e.reference) * 31 + e.data.hashCode()
//        }
//    }
}