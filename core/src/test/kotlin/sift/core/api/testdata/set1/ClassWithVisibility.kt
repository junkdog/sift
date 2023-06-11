package sift.core.api.testdata.set1

class ClassWithVisibilityA {
}

internal class ClassWithVisibilityB {
    internal fun fnInternal() = Unit
    private fun fnPrivate() = Unit
    protected fun fnProtected() = Unit
    fun fnPublic() = Unit
}

