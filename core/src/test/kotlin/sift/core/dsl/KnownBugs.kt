package sift.core.dsl

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sift.core.api.debugLog


private class NoTypeInfo<T> {
    fun unableToResolveT(param: T) {}
}

private class Oof {
    fun oof(repo: NoTypeInfo<String>) {

    }
}

private class Rab()

@Disabled
class KnownBugs {

    init {
        debugLog = true
    }

    @Test
    fun hmm() {

    }
}


