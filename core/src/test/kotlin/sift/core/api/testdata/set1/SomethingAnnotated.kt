package sift.core.api.testdata.set1

@AnnoPrimitives(true, 3, 4.toChar(), 5, 6, 7L, 3f, 4.0)
class SomethingAnnotated(
    val field: String
) {
    @field:AnnoPrimitives(false, 2, 3.toChar(), 4, 5, 6L, 2f, 3.0)
    val otherField: String = "hai"

    @AnnoPrimitives(true, 1, 2.toChar(), 3, 4, 5L, 1f, 2.0)
    fun foo(
        a: Int,
        @AnnoPrimitives(false, 3, 4.toChar(), 5, 6, 7L, 3f, 4.0)
        b: Int
    ) = Unit
}