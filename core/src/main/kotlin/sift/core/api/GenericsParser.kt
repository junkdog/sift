package sift.core.api

import sift.core.stringWriter
import sift.core.dsl.Dsl


internal fun explodeTypeFromSignature(
    context: Dsl.Signature,
    signature: String,
    synthesize: Boolean,
    f: Dsl.Classes.() -> Unit
) {
    // we'll only evaluate nodes leading to T
    fun containsT(sig: GenericType) = sig.flatten().any { it.constraint is TypeName.TypeT }

    val sig = parseGenericType(signature.replace(" ", ""))

    // require T in signature
    require(containsT(sig)) {
        "Signature must contain a type parameter T: $signature"
    }

    // T can only occur once
    require(sig.flatten().count { it.constraint is TypeName.TypeT } == 1) {
        "Type parameter T must only occur once: $signature"
    }

    fun recurse(dsl: Dsl.Signature, self: GenericType) {
        when (self.constraint) {
            is TypeName.TypeT -> {
                dsl.explodeType(synthesize, f)
            }
            is TypeName.SimpleName -> {
                dsl.filter(Regex("^.+\\.${self.constraint.name}\$"))
                self.arguments
                    .filter(::containsT)
                    .forEach { arg -> dsl.typeArgument(arg.index) { recurse(this, arg) } }
            }
            is TypeName.Any -> {
                self.arguments
                    .filter(::containsT)
                    .forEach { arg -> dsl.typeArgument(arg.index) { recurse(this, arg) } }
            }
        }
    }

    recurse(context, sig)
}

private fun parseGenericType(
    signature: String
): GenericType = GenericsParser(signature).parseType(0)

private sealed interface TypeName {
    data class SimpleName(val name: String) : TypeName {
        override fun toString(): String = name
    }

    object TypeT : TypeName {
        override fun toString(): String = "T"
    }

    object Any : TypeName {
        override fun toString(): String = "_"
    }

    companion object {
        fun from(s: String): TypeName = when (s) {
            "_"  -> Any
            "T"  -> TypeT
            else -> SimpleName(s)
        }
    }
}

private class GenericType(val index: Int, val constraint: TypeName, val arguments: MutableList<GenericType>) {
    override fun toString(): String = when (arguments.size) {
        0 -> constraint.toString()
        else -> "${constraint}<${arguments.joinToString(", ")}>"
    }

    fun flatten(): List<GenericType> = listOf(this) + arguments.flatMap { it.flatten() }
}


private class GenericsParser(val input: String) {
    private var index = 0

    fun parseType(index: Int): GenericType {
        val name = parseTypeName()
        return if (at('<')) {
            consume('<')
            val arguments = parseTypeArguments()
            consume('>')
            GenericType(index, name, arguments.toMutableList())
        } else {
            GenericType(index, name, mutableListOf())
        }
    }

    private fun consume(): Char = input[index++]

    private fun consume(c: Char) {
        if (!at(c)) {
            error("Expected character '$c' at index $index")
        }
        index++
    }

    private fun parseTypeName(): TypeName = TypeName.from(stringWriter {
        while (!(at('<') || at('>') || at(','))) {
            append(consume())
        }
    })

    private fun parseTypeArguments(): List<GenericType> {
        val arguments = mutableListOf<GenericType>()

        var index = 0
        while (!at('>')) {
            arguments.add(parseType(index++))
            if (at(',')) {
                consume(',')
            }
        }

        return arguments
    }

    private fun at(check: Char) = input.length > index && input[index] == check
}
