package sift.core.dsl

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.ClassNode
import sift.core.api.*
import sift.core.api.testdata.set2.Repo
import sift.core.api.testdata.set2.RepoT
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.template.SystemModelTemplate
import sift.core.template.load


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


