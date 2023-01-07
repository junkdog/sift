package sift.core.terminal

import com.fasterxml.jackson.annotation.JsonSubTypes
import sift.core.terminal.TextTransformer.Companion.idSequence
import sift.core.terminal.TextTransformer.Companion.uuidSequence
import java.util.UUID
import java.util.regex.Pattern

// applied before styling
@JsonSubTypes(
    JsonSubTypes.Type(Deduplicate::class, name = "dedupe"),
    JsonSubTypes.Type(IdSequencer::class, name = "id-sequence"),
    JsonSubTypes.Type(Replace::class, name = "replace"),
)
interface TextTransformer {
    operator fun invoke(s: String): String

    companion object {
        fun dedupe(char: Char): TextTransformer = Deduplicate(char)
        fun idSequence(regex: Regex, group: Int = 0): TextTransformer = IdSequencer(regex, group)
        fun uuidSequence(): TextTransformer = idSequence(uuidRegex)
        fun replace(text: String, with: String): TextTransformer = Replace(Pattern.quote(text).toRegex(), with)
        fun replace(regex: Regex, with: String): TextTransformer = Replace(regex, with)
    }
}

private class Replace(val regex: Regex, val with: String) : TextTransformer {
    override fun invoke(s: String): String = regex.replace(s, with)
}

private class IdSequencer(val pattern: Regex, val group: Int) : TextTransformer {
    private val id: UUID = UUID.randomUUID()

    override fun invoke(s: String): String {
        val extracted = state(id)
        return pattern.replace(s) { match ->
            val token = match.groups[group]?.value ?: error("unable to find token for group=$group: $match")
            extracted.getOrPut(token) { (extracted.size + 1).toString() }
        }
    }

    companion object {
        private val instances: MutableMap<UUID, MutableMap<String, String>> = mutableMapOf()

        private fun state(uuid: UUID): MutableMap<String, String> {
            return instances.getOrPut(uuid, ::mutableMapOf)
        }
    }
}

private class Deduplicate(
    val char: Char
) : TextTransformer {

    override fun invoke(s: String): String {
        return s.replace(Regex("$char+"), "$char")
    }
}

private val uuidRegex = Regex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")

