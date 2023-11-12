@file:Suppress("MemberVisibilityCanBePrivate")

package sift.core.dsl

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sift.core.asm.classNode
import sift.core.dsl.MethodSelection.inherited
import sift.core.entity.Entity
import sift.core.expecting
import sift.core.junit.LogActiveTestExtension

@ExtendWith(LogActiveTestExtension::class)
class ScopeInheritedElementsTest {
    val cns = listOf(
        classNode(Concrete::class),
        classNode(BaseCore::class),
        classNode(Base::class)
    )

    val cls = Entity.Type("class")
    val field = Entity.Type("field")
    val method = Entity.Type("method")

    @Test
    fun `resolve inherited fields`() {
        template {
            classes {
                entity(cls)
                fields(inherited = true) {
                    entity(field)
                }
            }

            fieldsOf(field) { e ->
                // it would have been easier to declare this relationship right after
                // the field entity was created; this is just to demonstrate that
                // the relationship can be declared at any point
                cls["fields"] = e
            }
        }.expecting(cns, cls, """
            ── class
               ├─ Base
               │  └─ Base.baseField
               ├─ BaseCore
               │  ├─ BaseCore.baseField
               │  └─ BaseCore.coreField
               └─ Concrete
                  ├─ Concrete.baseField
                  ├─ Concrete.concreteField
                  └─ Concrete.coreField
            """
        )
    }

    @Test
    fun `resolve inherited methods and field access from inherited classes`() {
        classes {
            filter("Concrete")
            entity(cls)
            fields(inherited = true) {
                entity(field)
            }
            methods(inherited) {
                // don't care about property getter or ctor
                filter(Regex("^(get|<init>)"), invert = true)

                entity(method)
                cls["methods"] = method
                method["invokedFields"] = field.fieldAccess
            }
        }.expecting(cns, cls, """
            ── class
               └─ Concrete
                  ├─ Concrete::base
                  │  └─ Concrete.baseField
                  ├─ Concrete::concrete
                  │  ├─ Concrete.baseField
                  │  ├─ Concrete.concreteField
                  │  └─ Concrete.coreField
                  └─ Concrete::fooCore
                     ├─ Concrete.baseField
                     └─ Concrete.coreField
            """
        )
    }
}


private open class Base {
    val baseField: String = "hello"

    fun base() {
        listOf(baseField)
    }
}

private open class BaseCore : Base() {
    val coreField: Int = 0

    fun fooCore() {
        listOf(baseField, coreField)
    }
}

private open class Concrete : BaseCore() {
    val concreteField: Boolean = true

    fun concrete() {
        listOf(baseField, coreField, concreteField)
    }
}
