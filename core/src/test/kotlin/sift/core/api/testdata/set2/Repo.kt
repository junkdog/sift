package sift.core.api.testdata.set2


// funs to be resolved from invocations in RepoClient
interface Repo {
    fun a()
    fun b()
    fun c()
}

class RepoImpl : Repo {
    fun d(): Unit = Unit
    override fun a() = Unit
    override fun b() = Unit
    override fun c() = Unit
}

class RepoClient() {
    fun callACandD(repo: RepoImpl) {
        repo.a()
        repo.c()
        repo.d()
    }

    fun callB(repo: RepoImpl) {
        repo.b()
    }
}

interface RepoT<T>
abstract class AbstractRepoT<T>

class GenericRepos {
    var iRepoInt: RepoT<Int> = object : RepoT<Int> {}
    var aRepoInt: RepoT<Int> = object : RepoT<Int> {}

    fun aRepoString(repoT: AbstractRepoT<String>) = Unit
    fun iRepoString(repoT: RepoT<String>) = Unit
}