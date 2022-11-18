package sift.core.api.testdata.set3

annotation class InlineMarker

class KotlinInliningIntrospection {

    @InlineMarker
    fun reify() {
        reifiedHof { Foo() }
    }

    @InlineMarker
    fun reifyNoinline() {
        reifiedNoinlinHof { Foo() }
    }

    @InlineMarker
    fun noinline() {
        noinlineHof { Foo() }
    }

    @InlineMarker
    fun noinlineDynamic() {
        val a = "omg"
        noinlineHof { Foo(a) }
    }

    @InlineMarker
    fun crossinline() {
        crossinlineHof { Foo() }
    }

    class Foo(val s: String = "hmm")
}

inline fun <reified T> reifiedNoinlinHof(noinline f: () -> T): T {
    return f()
}

inline fun <reified T> reifiedHof(f: () -> T): T {
    return f()
}

inline fun noinlineHof(noinline f: () -> Unit): Unit {
    return f()
}

inline fun crossinlineHof(crossinline f: () -> Unit): Unit {
    return f()
}
