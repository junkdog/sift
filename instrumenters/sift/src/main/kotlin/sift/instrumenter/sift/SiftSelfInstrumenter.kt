package sift.instrumenter.sift

import org.objectweb.asm.Type
import sift.core.api.Action
import sift.core.entity.Entity
import sift.core.api.Dsl.instrumenter
import sift.core.asm.type
import sift.instrumenter.InstrumenterService
import sift.instrumenter.Style
import sift.instrumenter.serialize
import java.io.File

typealias E = SiftSelfInstrumenter.EntityTypes
typealias T = SiftSelfInstrumenter.AsmTypes

@Suppress("unused")
class SiftSelfInstrumenter : InstrumenterService {
    override val entityTypes: Iterable<Entity.Type> = listOf(E.dsl, E.action, E.scope)
    override val defaultType: Entity.Type = entityTypes.first()

    object AsmTypes {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!

    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val action = "action".type
        val dsl = "dsl".type
        val scope = "scope".type
    }

    override fun create() = this
    override val name: String
        get() = "sift"

    override fun pipeline() = instrumenter {
        classes {
            implements(type<Action<*, *>>())
            entity(E.action)
        }
    }

    override fun theme() = mapOf<Entity.Type, Style>(
    )
}

fun save(instrumenter: InstrumenterService) {
    File("${System.getProperty("user.home")}/.local/share/sift/instrumenters")
        .also(File::mkdirs)
        .resolve("${instrumenter.name}.json")
        .writeText(instrumenter.serialize())

    println("installed --instrumenter ${instrumenter.name}")
}

fun main(args: Array<String>) {
    save(SiftSelfInstrumenter())
}
