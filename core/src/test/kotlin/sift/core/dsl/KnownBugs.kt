package sift.core.dsl

import org.junit.jupiter.api.Disabled
import org.objectweb.asm.tree.ClassNode
import sift.core.api.*
import sift.core.entity.EntityService

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
}

