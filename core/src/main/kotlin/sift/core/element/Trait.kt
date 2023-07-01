package sift.core.element

import sift.core.dsl.Type

object Trait {
    interface HasType {
        val type: Type
    }
}