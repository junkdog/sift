package sift.core.api.testdata.set1

@RestController
class SomeController {
    @Endpoint("/foo", "POST")
    fun create() = Unit
    @Endpoint("/bar", "DELETE")
    fun delete() = Unit

    @Query
    fun query() = Unit

    fun `not an endpoint`() = Unit
}

class NotAController {
    var field: Boolean = false

    fun `not an endpoint`() = Unit
}
