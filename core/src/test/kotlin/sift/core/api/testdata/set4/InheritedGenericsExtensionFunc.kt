package sift.core.api.testdata.set4


class Optional<T>(val value: T? = null) {
    fun orElse(default: T?): T? = value ?: default
}

interface CrudRepository<T, ID> {
    fun findById(id: ID): Optional<T>
    fun yolo()
}

fun <T, ID> CrudRepository<T, ID>.findByIdOrNull(id: ID): T? = findById(id).orElse(null)

class CrudRepositoryImpl : CrudRepository<String, Int> {
    override fun findById(id: Int): Optional<String> = Optional("hi")
    override fun yolo() = Unit
}

class CrudRepositoryImpl2 : CrudRepository<String, Int> {
    override fun findById(id: Int): Optional<String> = Optional("hi")
    override fun yolo() = Unit
}

class Service(val repo: CrudRepositoryImpl, val repo2: CrudRepositoryImpl2) {
    fun func() {
        repo.findByIdOrNull(213)
        repo.yolo()

        repo2.findByIdOrNull(0)
    }
}