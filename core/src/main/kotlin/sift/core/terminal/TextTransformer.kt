package sift.core.terminal

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.ajalt.mordant.rendering.TextStyle
import java.util.*
import java.util.regex.Pattern

internal class StringEditor(val transformers: List<TextTransformer>) {
    override fun toString(): String {
        fun format(transformer: TextTransformer): String = when (transformer) {
            is Deduplicate -> "dedupe(${transformer.char})"
            is IdSequencer -> "id-sequence(${transformer.pattern.pattern})"
            is Replace     -> "replace(${transformer.regex.pattern} -> ${transformer.with})"
            is TextEdit    -> "edit(${transformer.transformers.joinToString(transform = ::format)})"
        }

        return transformers.joinToString(", ", "(", ")", transform = ::format)
    }

    operator fun invoke(any: Any): String =
        transformers.fold(any.toString()) { acc, transformer -> transformer(acc) }
}

// applied before styling
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(TextEdit::class, name = "edit"),
    JsonSubTypes.Type(Deduplicate::class, name = "dedupe"),
    JsonSubTypes.Type(IdSequencer::class, name = "id-sequence"),
    JsonSubTypes.Type(Replace::class, name = "replace"),
)
sealed interface TextTransformer {
    operator fun invoke(s: String): String
    operator fun invoke(any: Any): String = invoke(any.toString())

    companion object {
        fun edit(vararg transformers: TextTransformer): TextTransformer = TextEdit(transformers.toList())
        fun dedupe(char: Char): TextTransformer = Deduplicate(char)
        fun idSequence(regex: Regex, group: Int = 0): TextTransformer = IdSequencer(regex, group)
        fun uuidSequence(): TextTransformer = idSequence(uuidRegex)
        fun replace(text: String, with: String): TextTransformer = Replace(Pattern.quote(text).toRegex(), with)
        fun replace(regex: Regex, with: String): TextTransformer = Replace(regex, with)
        fun stylize(style: TextStyle): TextTransformer = Replace(everythingRegex, style("\$0"))
    }
}

private class TextEdit(val transformers: List<TextTransformer>) : TextTransformer {
    override fun invoke(s: String): String = transformers.fold(s) { acc, transformer -> transformer(acc) }
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
private val everythingRegex = Regex(".+")

