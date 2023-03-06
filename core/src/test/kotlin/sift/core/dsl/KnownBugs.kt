package sift.core.dsl

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.ClassNode
import sift.core.api.*
import sift.core.entity.EntityService
import sift.core.template.SystemModelTemplate
import sift.core.template.load

@Disabled
class KnownBugs {

    init {
        debugLog = true
    }


    private fun Action<Unit, Unit>.execute(
            cns: List<ClassNode>,
            block: (EntityService) -> Unit
    ) {
        TemplateProcessor(cns)
            .process(this, false, Context::debugTrails)
            .entityService
            .also(block)
    }

    @Test
    fun hmm() {
        SystemModelTemplate.load("petclinic")
    }
}


