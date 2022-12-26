package sift.core.api.testdata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
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
        PipelineProcessor(cns)
            .processPipeline(this, false) { it.debugTrails() }
            .entityService
            .also(block)
    }
}

