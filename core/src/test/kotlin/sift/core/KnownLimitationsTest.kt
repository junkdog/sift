package sift.core

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.extension.ExtendWith
import org.objectweb.asm.tree.ClassNode
import sift.core.api.*
import sift.core.api.debug.debugTraces
import sift.core.entity.EntityService
import sift.core.junit.LogActiveTestExtension

@Disabled
@ExtendWith(LogActiveTestExtension::class)
class KnownLimitationsTest {

    init {
        debugLog = true
    }

    private fun Action<Unit, Unit>.execute(
        cns: List<ClassNode>,
        block: (EntityService) -> Unit
    ) {
        TemplateProcessor(cns)
            .process(this, false, Context::debugTraces)
            .entityService
            .also(block)
    }
}