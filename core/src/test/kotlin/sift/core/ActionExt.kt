package sift.core

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import org.assertj.core.api.Assertions
import org.objectweb.asm.tree.ClassNode
import sift.core.api.*
import sift.core.api.Context
import sift.core.api.debug.debugTraces
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.template.toTree

fun Action<Unit, Unit>.expecting(
    cns: List<ClassNode>,
    block: (EntityService) -> Unit
) {
    TemplateProcessor(cns)
        .process(this, true, Context::debugTraces)
        .entityService
        .also(block)
}

fun Action<Unit, Unit>.expecting(
    cns: List<ClassNode>,
    root: Entity.Type,
    expectTree: String
) {
    expecting(cns, listOf(root), expectTree)
}

fun Action<Unit, Unit>.expecting(
    cns: List<ClassNode>,
    root: List<Entity.Type>,
    expectTree: String
) {
    expecting(cns) { es ->
        Assertions.assertThat(es.toTree(root).let(noAnsi::render))
            .isEqualTo(expectTree.trimIndent() + "\n")
    }
}

private fun EntityService.toTree(roots: List<Entity.Type>): String {
    return SystemModel(this).toTree(roots).toString()
}

private val noAnsi = Terminal(ansiLevel = AnsiLevel.NONE)

