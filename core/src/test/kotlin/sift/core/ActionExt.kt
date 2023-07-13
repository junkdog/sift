package sift.core

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import org.assertj.core.api.Assertions
import org.objectweb.asm.tree.ClassNode
import sift.core.api.Action
import sift.core.api.Context
import sift.core.api.TemplateProcessor
import sift.core.api.debugTrails
import sift.core.dsl.toTree
import sift.core.entity.Entity
import sift.core.entity.EntityService

fun Action<Unit, Unit>.expecting(
    cns: List<ClassNode>,
    block: (EntityService) -> Unit
) {
    TemplateProcessor(cns)
        .process(this, true, Context::debugTrails)
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
    val noAnsi = Terminal(ansiLevel = AnsiLevel.NONE)
    expecting(cns) { es ->
        Assertions.assertThat(es.toTree(root).let(noAnsi::render))
            .isEqualTo(expectTree.trimIndent() + "\n")
    }
}