package sift.core.dsl

import sift.core.api.AccessFlags.acc_synthetic
import sift.core.dsl.MethodSelection.*
import sift.core.element.MethodNode

@Suppress("ClassName")
sealed interface MethodSelectionFilter {
    operator fun invoke(mn: MethodNode): Boolean
}

enum class MethodSelection : MethodSelectionFilter {
    /** Matches all constructors in a class. */
    constructors {
        override fun invoke(mn: MethodNode): Boolean {
            return mn.normalMethod && mn.name == "<init>"
        }
    },
    /** Matches all methods that are directly declared by the class, excluding constructors. */
    declared {
        override fun invoke(mn: MethodNode): Boolean {
            return (mn.isKotlin || !mn.owner.isKotlin) && mn.normalMethod && mn.name != "<init>"
        }
    },
    /** Matches all declared and inherited methods, excluding constructors. */
    inherited {
        override fun invoke(mn: MethodNode): Boolean {
            return mn.normalMethod && mn.name != "<init>"
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
    };
}

internal class CompositeSelection(
    val allow: Set<MethodSelection>
) : MethodSelectionFilter {

    val disallow: List<MethodSelection> by lazy {
        if (declared in allow || inherited in allow)
            defaultDisalow - allow
        else
            defaultDisalow + declared - allow
    }

    override fun invoke(mn: MethodNode): Boolean {
        return disallow.none { it(mn) } && allow.any { it(mn) }
    }

    override fun toString(): String = allow.joinToString(" + ")
}

operator fun MethodSelectionFilter.plus(rhs: MethodSelection): MethodSelectionFilter = when (this) {
    is CompositeSelection -> CompositeSelection(allow + rhs)
    is MethodSelection -> CompositeSelection(setOf(this, rhs))
}

private val defaultDisalow = listOf(constructors, synthetic,  accessors)

internal val MethodSelectionFilter.isInheriting: Boolean
    get() = this === inherited
        || (this is CompositeSelection && inherited in allow)

private val MethodNode.normalMethod: Boolean
    get() = '$' !in name && '-' !in name
