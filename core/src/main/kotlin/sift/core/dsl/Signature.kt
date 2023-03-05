package sift.core.dsl

import sift.core.api.*

/**
 * Signature scope for working with generics.
 *
 * @see [Classes.superclass]
 * @see [Fields.signature]
 * @see [Methods.returns]
 * @see [Parameters.signature]
 */
@SiftTemplateDsl
class Signature internal constructor(
    var action: Action.Chain<IterSignatures> = chainFrom(Action.Signature.SignatureScope)
) {
    fun readName(): Action<IterSignatures, IterValues> {
        val forkTo = Action.Signature.ReadSignature
            .let { Action.Fork(it) }

        action +=  forkTo

        return forkTo.forked
    }

    fun scope(label: String, f: Signature.() -> Unit) {
        action += Action.Fork(Signature().also(f).action)
    }

    fun filter(s: String, invert: Boolean = false) {
        action += Action.Signature.Filter(Regex.fromLiteral(s), invert)
    }

    fun filter(regex: Regex, invert: Boolean = false) {
        action += Action.Signature.Filter(regex, invert)
    }

    fun typeArguments(f: Signature.() -> Unit) {
        val inner = Signature().also(f).action
        action += Action.Fork(Action.Signature.InnerTypeArguments andThen inner)
    }

    fun typeArgument(index: Int, f: Signature.() -> Unit) {
        val filterNth = Action.Signature.FilterNth(index)
        val forkTo = Signature().also(f).action

        action += Action.Fork(filterNth andThen forkTo)
    }

    fun explodeType(synthesize: Boolean = false, f: Classes.() -> Unit) {
        val explodeType = Action.Signature.ExplodeType(synthesize)
        val forkTo = Classes().also(f).action

        action += Action.Fork(explodeType andThen forkTo)
    }

    /**
     * Iterates over all classes given a generic type signature, e.g.
     * `Map<_, List<T>>`. The signature parameter describes the generic
     * type to search for. It must contain a `T` token, which will be
     * replaced with each declaration during iteration.  The `_` symbol
     * can be used to match any class.
     *
     * Type constraints with names (e.g. String, Map) are only applied
     * to direct ancestors of `T`. In the signature `Pair<Foo, List<Map<Bar, T>>>`,
     * `Foo` and `Bar` are not directly related `T` and will therefore not be
     * evaluated.
     *
     * This function can greatly reduce the boilerplate associated with manually
     * unpacking type signatures. The following two templates are equivalent:
     *
     * ```kotlin
     * val a = classes {
     *     methods {
     *         returns {
     *             explodeTypeT("Map<_, List<Pair<T, _>>>", synthesize = true) {
     *                 entity(payload)
     *             }
     *         }
     *     }
     * }
     *
     * val b = classes {
     *     methods {
     *         returns {
     *             filter(Regex("^.+\\.Map\$"))
     *             typeArgument(1) {                     // List<Pair<Payload, Int>>
     *                 filter(Regex("^.+\\.List\$"))
     *                 typeArgument(0) {                 // Pair<Payload, Int>
     *                     filter(Regex("^.+\\.Pair\$"))
     *                     typeArgument(0) {             // Payload
     *                         explodeType(synthesize = true) {
     *                             entity(payload)
     *                         }
     *                     }
     *                 }
     *             }
     *         }
     *     }
     * }
     *
     * assert(a == b) { "expecting a and b to have the same underlying representation" }
     * ```
     */
    fun explodeTypeT(
        signature: String = "_<T>",
        synthesize: Boolean = false,
        f: Classes.() -> Unit
    ) {
        explodeTypeFromSignature(this, signature, synthesize, f)
    }

    /**
     * When `--debug` is passed to the CLI, prints [tag] and all elements
     * currently in scope.
     *
     * Note that for most use-cases, `--profile` yields better results
     * without having to modify the pipeline.
     **/
    fun log(tag: String) {
        action += Action.DebugLog(tag)
    }

    /**
     * When `--debug` is passed to the CLI, prints [tag] and the count
     * of elements currently in scope.
     *
     * Note that for most use-cases, `--profile` yields better results
     * without having to modify the template.
     **/
    fun logCount(tag: String) {
        action += Action.DebugLog(tag, format = Action.DebugLog.LogFormat.Count)
    }
}