package sift.core.dsl

import sift.core.api.AccessFlags.acc_synthetic
import sift.core.dsl.MethodSelection.*
import sift.core.element.MethodNode

sealed interface MethodSelectionFilter {
    operator fun invoke(mn: MethodNode): Boolean
}

enum class MethodSelection : MethodSelectionFilter {
    /** Matches all constructors in a class. */
    constructors {
        override fun invoke(mn: MethodNode): Boolean {
            return mn.name == "<init>"
        }
    },
    /** Matches class initialization blocks. */
    staticInitializers {
        override fun invoke(mn: MethodNode): Boolean {
            return mn.name == "<clinit>"
        }
    },
    /** Matches all methods that are directly declared by the class, excluding constructors. */
    declared {
        override fun invoke(mn: MethodNode): Boolean {
            return (mn.isKotlin || !mn.owner.isKotlin) && mn.normalMethod
        }
    },
    /** Matches all declared and inherited methods, excluding constructors. */
    inherited {
        override fun invoke(mn: MethodNode): Boolean {
            return mn.normalMethod
                && (mn.originalCn == null || mn.visibility >= Visibility.Protected)
        }
    },
    /** Matches synthetic methods, such as default-value functions in Kotlin. */
    synthetic {
        override fun invoke(mn: MethodNode): Boolean {
            return acc_synthetic.check(mn.access)
        }
    },
    /** Matches Kotlin's property getters and setters. */
    accessors {
        override fun invoke(mn: MethodNode): Boolean {
            return mn.normalMethod && (!mn.isKotlin && mn.owner.isKotlin)
                && (mn.name.startsWith("get") || mn.name.startsWith("set"))
        }
    },
    abstractMethods {
        override fun invoke(mn: MethodNode): Boolean {
            return mn.isAbstract && mn.normalMethod
        }
    };
}

internal class MethodSelectionSet(
    val allow: Set<MethodSelection>
) : MethodSelectionFilter {

    // fixme: disallow set should update more intelligently
    val disallow: List<MethodSelection> by lazy {
        if (declared in allow || inherited in allow || abstractMethods in allow)
            defaultDisallow - allow
        else
            defaultDisallow + declared - allow
    }

    override fun invoke(mn: MethodNode): Boolean {
        return disallow.none { it(mn) } && allow.any { it(mn) }
    }

    override fun toString(): String = allow.joinToString(" + ")
}

operator fun MethodSelectionFilter.plus(rhs: MethodSelection): MethodSelectionFilter = when (this) {
    is MethodSelectionSet -> MethodSelectionSet(allow + rhs)
    is MethodSelection -> MethodSelectionSet(setOf(this, rhs))
}

private val defaultDisallow = listOf(constructors, synthetic,  accessors, abstractMethods)

internal val MethodSelectionFilter.isInheriting: Boolean
    get() = this === inherited
        || (this is MethodSelectionSet && inherited in allow)

private val MethodNode.normalMethod: Boolean
    get() = '$' !in name && '-' !in name && !name.startsWith("<")
