package sift.core.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import sift.core.*
import sift.core.EntityNotFoundException
import sift.core.UniqueElementPerEntityViolation
import sift.core.api.Dsl.classes
import sift.core.api.Dsl.instrumenter
import sift.core.api.ScopeEntityPredicate.ifExists
import sift.core.api.ScopeEntityPredicate.ifExistsNot
import sift.core.api.testdata.set1.*
import sift.core.api.testdata.set2.*
import sift.core.asm.classNode
import sift.core.asm.type
import sift.core.entity.Entity
import sift.core.entity.EntityService
import java.io.InputStream
import kotlin.test.assertTrue

typealias JavaDeprecated = java.lang.Deprecated

fun resource(path: String): InputStream {
    return DslTest::class.java.getResourceAsStream(path)!!
}

@Suppress("UNCHECKED_CAST")
class DslTest {

    init {
        debugLog = true
    }

    val allCns = listOf(
        classNode<SomeController>(),
        classNode<RecursiveInstantiations>(),
        classNode<NotAController>(),
        classNode<SomethingAnnotated>(),
        classNode<MethodWithParam>(),

        // annotations (junk)
        classNode<Endpoint>(),
        classNode<RestController>(),

        // more junk
        classNode<DslTest>()
    )

    @Test @Disabled
    fun `read class and enum from annotation`() {
        TODO()
    }

    @Test
    fun `turn into first cli`() {
        val klazz = Entity.Type("class")
        val method = Entity.Type("method")
        val field = Entity.Type("field")

        classes {
            scope("deprecated java classes") {
                annotatedBy<JavaDeprecated>()
                entity(klazz)
            }
            scope("deprecated kotlin classes") {
                annotatedBy<Deprecated>()
                entity(klazz)
            }
            methods {
                annotatedBy<JavaDeprecated>()
                entity(method)
            }
            methods {
                annotatedBy<Deprecated>()
                entity(method)
            }
            fields {
                annotatedBy<JavaDeprecated>()
                entity(field)
            }
            fields {
                annotatedBy<Deprecated>()
                entity(field)
            }
        }.execute {}
    }

    @Test
    fun `read primitive values from annotation`() {
        val klazz = Entity.Type("klazz")
        val method = Entity.Type("method")
        val field = Entity.Type("field")
        val parameter = Entity.Type("param")

        classes {
            annotatedBy<AnnoPrimitives>()
            entity(klazz,
                property("bool", readAnnotation(AnnoPrimitives::bool)),
                property("byte", readAnnotation(AnnoPrimitives::byte)),
                property("char", readAnnotation(AnnoPrimitives::char)),
                property("short", readAnnotation(AnnoPrimitives::short)),
                property("int", readAnnotation(AnnoPrimitives::int)),
                property("long", readAnnotation(AnnoPrimitives::long)),
                property("float", readAnnotation(AnnoPrimitives::float)),
                property("double", readAnnotation(AnnoPrimitives::double)))

            methods {
                annotatedBy<AnnoPrimitives>()
                entity(method,
                    property("bool", readAnnotation(AnnoPrimitives::bool)),
                    property("byte", readAnnotation(AnnoPrimitives::byte)),
                    property("char", readAnnotation(AnnoPrimitives::char)),
                    property("short", readAnnotation(AnnoPrimitives::short)),
                    property("int", readAnnotation(AnnoPrimitives::int)),
                    property("long", readAnnotation(AnnoPrimitives::long)),
                    property("float", readAnnotation(AnnoPrimitives::float)),
                    property("double", readAnnotation(AnnoPrimitives::double)))

                parameters {
                    annotatedBy<AnnoPrimitives>()
                    entity(parameter,
                        property("bool", readAnnotation(AnnoPrimitives::bool)),
                        property("byte", readAnnotation(AnnoPrimitives::byte)),
                        property("char", readAnnotation(AnnoPrimitives::char)),
                        property("short", readAnnotation(AnnoPrimitives::short)),
                        property("int", readAnnotation(AnnoPrimitives::int)),
                        property("long", readAnnotation(AnnoPrimitives::long)),
                        property("float", readAnnotation(AnnoPrimitives::float)),
                        property("double", readAnnotation(AnnoPrimitives::double)))

                }
            }

            fields {
                annotatedBy<AnnoPrimitives>()
                entity(field,
                    property("bool", readAnnotation(AnnoPrimitives::bool)),
                    property("byte", readAnnotation(AnnoPrimitives::byte)),
                    property("char", readAnnotation(AnnoPrimitives::char)),
                    property("short", readAnnotation(AnnoPrimitives::short)),
                    property("int", readAnnotation(AnnoPrimitives::int)),
                    property("long", readAnnotation(AnnoPrimitives::long)),
                    property("float", readAnnotation(AnnoPrimitives::float)),
                    property("double", readAnnotation(AnnoPrimitives::double)))
            }
        }.execute { entityService ->
            assertThat(entityService[klazz].values.map(Entity::toString))
                .hasSize(1)
                .first()
                .isEqualTo(
                    "Entity(SomethingAnnotated, type=klazz," +
                        " bool=true, byte=3, char=\u0004, short=5, int=6, long=7, float=3.0, double=4.0)"
                )
            assertThat(entityService[field].values.map(Entity::toString))
                .hasSize(1)
                .first()
                .isEqualTo(
                    "Entity(SomethingAnnotated.otherField, type=field," +
                        " bool=false, byte=2, char=\u0003, short=4, int=5, long=6, float=2.0, double=3.0)"
                )
            assertThat(entityService[method].values.map(Entity::toString))
                .hasSize(1)
                .first()
                .isEqualTo(
                    "Entity(SomethingAnnotated::foo, type=method," +
                        " bool=true, byte=1, char=\u0002, short=3, int=4, long=5, float=1.0, double=2.0)"
                )
            assertThat(entityService[parameter].values.map(Entity::toString))
                .hasSize(1)
                .first()
                .isEqualTo(
                    "Entity(SomethingAnnotated::foo(b: int), type=param," +
                        " bool=false, byte=3, char=\u0004, short=5, int=6, long=7, float=3.0, double=4.0)"
                )
        }
    }

    @Test
    fun `entity assignment via parentScope`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        classes {
            methods {
                annotatedBy<Endpoint>()
                entity(endpoint)

                parentScope("register controller class") {
                    log("iterating set of classes with @Endpoint methods")
                    entity(controller)
                    controller["endpoints"] = endpoint
                }
            }
        }.execute { es ->
            assertThat(es[controller].map(::TestEntity))
                .hasSize(1)
                .first()
                .isEqualTo(e(controller, "SomeController", children = mapOf("endpoints" to listOf(
                    e(endpoint, "SomeController::create"),
                    e(endpoint, "SomeController::delete"),
                ))))
        }
    }

    @Test
    fun `entity assignment from method to class via parentScope`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        classes {
            methods {
                annotatedBy<Endpoint>()
                logCount("found endpoints")
                entity(endpoint)

                parentScope("register controller class") {
                    log("iterating set of classes with @Endpoint methods")
                    entity(controller)
                    controller["endpoints"] = endpoint
                }
            }
        }.execute { es ->
            assertThat(es[controller].map(::TestEntity))
                .hasSize(1)
                .first()
                .isEqualTo(e(controller, "SomeController", children =
                    mapOf("endpoints" to listOf(
                        e(endpoint, "SomeController::create"),
                        e(endpoint, "SomeController::delete"),
                    )
                )))
        }
    }

    @Test
    fun `scope to methods of registered entities`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        instrumenter {
            classes {
                methods {
                    annotatedBy<Endpoint>()
                    entity(endpoint)
                }
            }

            methodsOf(endpoint) {
                parentScope("register controller") {
                    entity(controller)
                    controller["endpoints"] = endpoint
                }
            }
        }.execute { es ->
            assertThat(es[controller].map(::TestEntity))
                .hasSize(1)
                .first()
                .isEqualTo(e(controller, "SomeController", children =
                mapOf("endpoints" to listOf(
                    e(endpoint, "SomeController::create"),
                    e(endpoint, "SomeController::delete"),
                ))))
        }
    }

    @Test
    fun `update entity properties from sub-scope`() {
        val cns: List<ClassNode> = listOf(
            classNode<MethodsWithTypes>(),
        )

        val et = Entity.Type("e")

        classes {
            log("classes")
            entity(et)
            methods {
                filter(Regex("mixedTypes"))
                parameters {
                    parameter(1)
                    log("after")
                    update(et, "a-parameter-type", readType())
                }
            }
        }.execute(cns) { es ->
            val entities = es[et]
            assertThat(entities)
                .hasSize(1)

            assertThat(entities.values.first().properties["a-parameter-type"])
                .isEqualTo(listOf(type<String>()))
        }
    }

    @Test
    fun `read type of class and parameters`() {
        val cns: List<ClassNode> = listOf(
            classNode<MethodsWithTypes>(),
        )

        val et = Entity.Type("class")

        classes {
            entity(et, property("type", readType()))
            methods {
                filter(Regex("mixedTypes"))
                parameters {
                    entity(et, property("type", readType()))
                }
            }
        }.execute(cns) { es ->
            val entities = es[et]
            assertThat(entities)
                .hasSize(3)

            val readTypes = entities.flatMap { (_, v) -> v.properties["type"]!! }
            assertThat(readTypes)
                .containsAll(listOf(
                    type<MethodsWithTypes>(),
                    type<MethodsWithTypes.Foo>(),
                    type<String>(),
                ))
        }
    }

    @Test
    fun `explode parameter types into class scope`() {
        val cns: List<ClassNode> = listOf(
            classNode<MethodsWithTypes>(),
            classNode<MethodsWithTypes.Foo>(),
            classNode<MethodsWithTypes.Bar>(),
        )

        val paramType = Entity.Type("param-type")

        classes {
            methods {
                filter(Regex("presentTypes"))
                parameters {
                    explodeType {
                        log("exploding types")
                        entity(paramType)
                    }
                }
            }
        }.execute(cns) { es ->
            assertThat(es[paramType].map(::TestEntity))
                .hasSize(2)
                .containsAll(
                    listOf(
                        e(paramType, "MethodsWithTypes.Foo"),
                        e(paramType, "MethodsWithTypes.Bar")
                    )
                )
        }

        classes {
            methods {
                filter(Regex("mixedTypes"))
                parameters {
                    explodeType {
                        entity(paramType)
                    }
                }
            }
        }.execute(cns) { es ->
            assertThat(es[paramType].map(::TestEntity))
                .hasSize(1)
                .containsAll(
                    listOf(
                        e(paramType, "MethodsWithTypes.Foo"),
                    )
                )
        }

        classes {
            methods {
                filter(Regex("noPresentTypes"))
                parameters {
                    explodeType {
                        entity(paramType)
                    }
                }
            }
        }.execute(cns) { es ->
            assertTrue(paramType !in es)
        }
    }

    @Test
    fun `recursively scan for instantiations`() {
        val cns = listOf(
            classNode<Instantiations>(),
            classNode<Instantiations.Yolo>(),
            classNode<Payload>(),
            classNode<Instantiations2>(),
        )

        val payload = Entity.Type("payload")
        val method = Entity.Type("method")

        classes {
            scope("register payload type") {
                filter(Regex("Payload"))
                entity(payload)
            }

            filter(Regex("Instantiations"))
            methods {
                entity(method)
                instantiationsOf(payload) {
                    logCount("instantiations")
                    method["instantiates"] = payload
                    payload["instantiated-by"] = method
                }
            }
        }.execute(cns) { es ->
            assertThat(es[payload])
                .hasSize(1)

            val payloadInstantiatedBy = es[payload]
                .map { (_, e) -> e }
                .first()
                .children["instantiated-by"]!!

            assertThat(payloadInstantiatedBy.prettyPrint())
                .containsExactlyInAnyOrder(
                    "Entity(Instantiations::caseA, type=method)",
                    "Entity(Instantiations::caseB, type=method)",
                    "Entity(Instantiations.Yolo::hmm, type=method)",
                    "Entity(Instantiations2::a, type=method)",
                    "Entity(Instantiations2::b, type=method)",
                    "Entity(Instantiations2::c, type=method)",
                )

            val payloadMethods = es[method]
                .map { (_, v) -> v }
                .filter { "instantiates" in it.children }

            assertThat(payloadMethods.flatMap { it.children("instantiates") }.toSet())
                .hasSize(1)

            assertThat(payloadMethods.prettyPrint())
                .containsExactlyInAnyOrder(
                    "Entity(Instantiations::caseA, type=method)",
                    "Entity(Instantiations::caseB, type=method)",
                    "Entity(Instantiations.Yolo::hmm, type=method)",
                    "Entity(Instantiations2::a, type=method)",
                    "Entity(Instantiations2::b, type=method)",
                    "Entity(Instantiations2::c, type=method)",
                )
        }
    }

    @Test @Disabled
    fun `read annotation from generic parameter`() {
        TODO()
    }

    @Test @Disabled
    fun `read generic type`() {
        TODO()
    }

    @Test @Disabled
    fun `treat constructor properties as fields`() {
        TODO()
    }

    @Test
    fun `construct label with readName`() {
        val cns: List<ClassNode> = listOf(
            classNode<MethodsWithTypes>(),
        )

        val et = Entity.Type("class")

        classes {
            entity(et, label("CLS \${name}"),
                property("name", readName()))
        }.execute(cns) { es ->
            val e = es[et].map { (_, e) -> e }
            assertThat(e).hasSize(1)
            assertThat(e.first().label)
                .isEqualTo("CLS MethodsWithTypes")


        }
    }

    @Test
    fun `filter classes`() {
        val controller = Entity.Type("controller")

        classes {
            filter(Regex("^sift\\.core\\.api\\.testdata"))

            annotatedBy<RestController>()
            entity(controller)
        }.execute { entityService ->
            assertThat(entityService[controller].map(::TestEntity))
                .hasSize(1)
                .first()
                .isEqualTo(
                    e(controller, "SomeController")
                )
        }
    }

    @Test
    fun `read endpoint and construct label`() {
        val endpoint = Entity.Type("endpoint")

        instrumenter {
            classes {
                filter(Regex("^sift\\.core\\.api\\.testdata"))

                methods {
                    annotatedBy<Endpoint>()
                    entity(endpoint, label("\${http-method} \${path}"),
                        property("http-method", readAnnotation(Endpoint::method)),
                        property("path", readAnnotation(Endpoint::path))
                    )
                }
            }
        }.execute { entityService ->
            assertThat(entityService[endpoint].map(::TestEntity))
                .hasSize(2)
                .containsAll(
                    listOf(
                        e(type = endpoint, label = "POST /foo",
                            "http-method" to "POST",
                            "path" to "/foo"),
                        e(type = endpoint, label = "DELETE /bar",
                            "http-method" to "DELETE",
                            "path" to "/bar"),
                        )
                    )
            }
        }


    @Test
    fun `synthesize missing classes for entity tagging`() {

        val cns = listOf(
            classNode<HandlerFn>(),
            classNode<HandlerOfFns>(),
        )

        val handler = Entity.Type("handler")
        val data = Entity.Type("data")

        fun validate(es: EntityService) {
            val handlers = es[handler].values.toList()
            assertThat(handlers).hasSize(2)
            assertThat(es[data].values.toList()).hasSize(1)

            val boo = handlers
                .first { "HandlerOfFns::boo" in it.label }

            val payload = es[data].values.toList().first()

            assertThat(boo.children("instantiates"))
                .hasSize(1)
                .first()
                .isEqualTo(payload)
        }

        // synthesize scope
        instrumenter {

            synthesize {
                entity(data, type<Payload>())
            }

            classes {
                methods {
                    annotatedBy<HandlerFn>()
                    entity(handler)

                    parameters {
                        parameter(0)
                        explodeType {
                            entity(data)
                        }
                    }

                    handler["instantiates"] = data.instantiations
                }
            }
        }.execute(cns, ::validate)

        // inline synthesize
        instrumenter {
            classes {
                methods {
                    annotatedBy<HandlerFn>()
                    entity(handler)

                    parameters {
                        parameter(0)
                        explodeType(synthesize = true) {
                            entity(data)
                        }
                    }

                    handler["instantiates"] = data.instantiations
                }
            }
        }.execute(cns, ::validate)
    }

    @Test
    fun `find implementations of generic interfaces`() {
        val cns = listOf(
            classNode<GenericInterfaceImpl>(),
            classNode<HandlerOfFns>(),
        )

        val impl = Entity.Type("implementer")

        classes {
            implements(Type.getType("Lsift.core.api.testdata.set2.GenericInterface;".replace('.', '/')))
            entity(impl)
        }.execute(cns) { es ->
            assertThat(es[impl].map { (_, e) -> e}.map(::TestEntity))
                .hasSize(1)
                .contains(e(impl, "GenericInterfaceImpl"))
        }
    }

    @Test
    fun `find all interfaces and parent-types`() {
        val cns = listOf(
            classNode<Interfaces.A>(),
            classNode<Interfaces.B>(),
            classNode<Interfaces.C>(),
            classNode<Interfaces.D>(),
            classNode<Interfaces.Base>(),
            classNode<Interfaces.ImplementorA>(),
            classNode<Interfaces.ImplementorB>(),
            classNode<Interfaces.ImplementorC>(),
            classNode<Interfaces.ImplementorD>(),
        )

        val base = Entity.Type("base")

        classes {
            filter(Regex("Implementor"))
            implements(type<Interfaces.Base>())
            entity(base)
        }.execute(cns) { es ->
            assertThat(es[base].map { (_, e) -> e }.map(::TestEntity))
                .containsExactlyInAnyOrder(
                    e(base, "Interfaces.ImplementorB"),
                    e(base, "Interfaces.ImplementorC"),
                    e(base, "Interfaces.ImplementorD"),
                )
        }
    }

    @Test
    fun `synthesize missing inherited methods`() {
        val cns = listOf(
            classNode<RepoImpl>(),
            classNode<RepoClient>(),
        )

        val repo = Entity.Type("repo")
        val fn = Entity.Type("repo-method")

        instrumenter {
            classes {
                implements(type<Repo>())
                log("repository")
                entity(repo)
            }

            classes {
                methods {
                    log("inspect")
                    invocationsOf(repo, synthesize = true) {
                        log("invoked")
                        entity(fn)
                        repo["methods"] = fn
                    }
                }
            }
        }.execute(cns) { es ->
            val fns = es[fn].map { (_, e) -> e }
            assertThat(fns)
                .hasSize(4)

            assertThat(es[repo])
                .hasSize(1)

            val repoImpl = es[repo].values.first()
            assertThat(repoImpl.children("methods"))
                .hasSize(4)
        }
    }

    @Test
    fun `child assignment using dot-invocations`() {
        val cns = listOf(
            classNode<HandlerFn>(),
            classNode<Payload>(),
            classNode<HandlerOfFns>(),
        )

        val method = Entity.Type("handler")
        val data = Entity.Type("data")

        classes {
            methods {
                filter(Regex("HandlerOfFns\$"))
                entity(method)

                method["invokes"] = method.invocations
            }
        }.execute(cns) { es ->
            val methods = es[method].values.toList()
            assertThat(methods).hasSize(5)

            fun er(s: String): Entity = methods.first { s in it.label }

            val invoker = er("HandlerOfFns::invoker")
            assertThat(invoker.children("invokes"))
                .hasSize(2)
                .containsExactlyInAnyOrder(
                    er("HandlerOfFns::on"),
                    er("HandlerOfFns::boo")
                )
        }
    }

    @Test
    fun `child assignment using dot-instantiations`() {
        val cns = listOf(
            classNode<HandlerFn>(),
            classNode<Payload>(),
            classNode<HandlerOfFns>(),
        )

        val handler = Entity.Type("handler")
        val data = Entity.Type("data")

        classes {
            methods {
                annotatedBy<HandlerFn>()
                entity(handler)

                parameters {
                    parameter(0)
                    explodeType {
                        entity(data)
                    }
                }

                handler["instantiates"] = data.instantiations
            }
        }.execute(cns) { es ->
            val handlers = es[handler].values.toList()
            assertThat(handlers).hasSize(2)
            assertThat(es[data].values.toList()).hasSize(1)

            val boo = handlers
                .first { "HandlerOfFns::boo" in it.label }

            val payload = es[data].values.toList().first()

            assertThat(boo.children("instantiates"))
                .hasSize(1)
                .first()
                .isEqualTo(payload)
        }
    }

    @Test
    fun `iterate registered class entities`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        instrumenter {
            classes {
                filter(Regex("^sift\\.core\\.api\\.testdata"))
                annotatedBy<RestController>()
                entity(controller)
            }

            classesOf(controller) {
                methods {
                    annotatedBy<Endpoint>()
                    entity(endpoint, label("\${http-method} \${path}"),
                        property("http-method", readAnnotation(Endpoint::method)),
                        property("path", readAnnotation(Endpoint::path)))
                }
            }
        }.execute { entityService ->
            assertThat(entityService[endpoint].map(::TestEntity))
                .hasSize(2)
                .containsExactlyInAnyOrder(
                    e(endpoint, "POST /foo",
                            "http-method" to "POST",
                            "path" to "/foo"),
                    e(endpoint, "DELETE /bar",
                        "http-method" to "DELETE",
                        "path" to "/bar"),
                )
        }
    }

    @Test @Disabled
    fun `correctly identify relations when scanning instantiations`() {
        val cns = listOf(
            classNode<HandlerFn>(),
            classNode<Payload>(),
            classNode<HandlerOfFns>(),
        )

        val handler = Entity.Type("handler")
        val data = Entity.Type("data")

        classes {
            scope("scan handler") {
                methods {
                    annotatedBy<HandlerFn>()
                    entity(handler)

                    parameters {
                        parameter(0)
                        explodeType {
                            entity(data)
                        }
                    }

//                    data["sent-by"] = handler

                    // works; reverse lookup via "backtrack" children
//                    handler["sent-by"] = data.instantiations

                    instantiationsOf(data) {
                        log("data")
                        data["sent-by"] = handler
                    }
                }
            }
        }.execute(cns) { es ->
            assertThat(es[data].values.first().children["sent-by"]!!.map(Entity::toString))
                .containsExactlyInAnyOrder(
                    e(handler, "HandlerOfFns::boo").toString(),
                )
        }
    }

    @Test
    fun `correctly identify relations when scanning invocations`() {
        val cns = listOf(
            classNode<HandlerFn>(),
            classNode<Payload>(),
            classNode<HandlerOfFns>(),
        )

        val handler = Entity.Type("handler")
        val invoker = Entity.Type("invoker")

        classes {
            scope("scan handler") {
                methods {
                    annotatedBy<HandlerFn>()
                    entity(handler)
                }
            }
            scope("scan invoker") {
                methods {
                    log("methods")
                    invokes(handler)
                    log("handler-invokers")
                    entity(invoker)
                    invoker["invokes"] = handler
                    handler["invoked-by"] = invoker
                }
            }
        }.execute(cns) { es ->
            val handlers = es[handler].values.toList()
            val invoker = es[invoker].values.toList()

            assertThat(invoker).hasSize(1)
            assertThat(handlers).hasSize(2)

            assertThat(invoker.first().children["invokes"]!!.map(Entity::toString))
                .containsExactlyInAnyOrder(
                    e(handler, "HandlerOfFns::boo").toString(),
                    e(handler, "HandlerOfFns::on").toString(),
                )
        }
    }

    @Test
    fun `associate controller as parent of endpoints`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        classes {
            filter(Regex("^sift\\.core\\.api\\.testdata"))
            annotatedBy<RestController>()
            entity(controller)
            methods {
                annotatedBy<Endpoint>()
                entity(endpoint, label("\${http-method} \${path}"),
                    property("http-method", readAnnotation(Endpoint::method)),
                    property("path", readAnnotation(Endpoint::path)))

                controller["endpoints"] = endpoint
            }
        }.execute { entityService ->
            assertThat(entityService[controller].map(::TestEntity))
                .hasSize(1)
                .first()
                .isEqualTo(e(controller, "SomeController",
                    children = mapOf("endpoints" to listOf(
                        e(endpoint, "POST /foo",
                            "http-method" to "POST",
                            "path" to "/foo"),
                        e(endpoint, "DELETE /bar",
                            "http-method" to "DELETE",
                            "path" to "/bar"),
                    ))
                ))
        }
    }

    @Test
    fun `mix and match pipelines`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        val pipelineA = classes {
            filter(Regex("^sift\\.core\\.api\\.testdata"))
            annotatedBy<RestController>()
            entity(controller)
        }

        val pipelineB = instrumenter {
            methodsOf(controller) {
                annotatedBy<Endpoint>()
                entity(endpoint, label("\${http-method} \${path}"),
                    property("http-method", readAnnotation(Endpoint::method)),
                    property("path", readAnnotation(Endpoint::path)),
                )

                controller["endpoints"] = endpoint
            }
        }

        instrumenter {
            include(pipelineA)
            include(pipelineB)
        }.execute { entityService ->
            assertThat(entityService[controller].map(::TestEntity))
                .hasSize(1)
                .first()
                .isEqualTo(e(controller, "SomeController",
                    children = mapOf("endpoints" to listOf(
                        e(endpoint, "POST /foo",
                            "http-method" to "POST",
                            "path" to "/foo"),
                        e(endpoint, "DELETE /bar",
                            "http-method" to "DELETE",
                            "path" to "/bar"),
                    ))
                ))
        }
    }

    @Test
    fun `read annotations from forked subsets using scope`() {
        val endpoint = Entity.Type("endpoint")
        val query = Entity.Type("query")

        classes {
            filter(Regex("^sift\\.core\\.api\\.testdata"))
            annotatedBy<RestController>()

            methods {
                scope("scan endpoints") {
                    annotatedBy<Endpoint>()
                    entity(endpoint, label("\${http-method} \${path}"),
                        property("http-method", readAnnotation(Endpoint::method)),
                        property("path", readAnnotation(Endpoint::path))
                    )

                    log("Endpoints")
                }
                scope("scan queries ") {
                    annotatedBy<Query>()
                    entity(query)
                }
            }
        }.execute { entityService ->
            assertThat(entityService[endpoint].map(::TestEntity))
                .hasSize(2)
                .containsAll(
                    listOf(
                        e(type = endpoint, label = "POST /foo",
                            "http-method" to "POST",
                            "path"        to "/foo"),
                        e(type = endpoint, label = "DELETE /bar",
                            "http-method" to "DELETE",
                            "path"        to "/bar"),
                    )
                )
            assertThat(entityService[query].map(::TestEntity))
                .hasSize(1)
                .containsAll(
                    listOf(
                        e(type = query, label = "SomeController::query")
                    )
                )
        }
    }

    @Test
    fun `filter on all node elements`() {
        val cls = Entity.Type("cls")
        val field = Entity.Type("field")
        val method = Entity.Type("method")
        val param = Entity.Type("param")

        classes {
            filter(Regex("MethodWithParam"))
            entity(cls)
            fields {
                filter(Regex("helloField"))
                entity(field)
            }
            methods {
                filter(Regex("fn"))
                entity(method)
                parameters {
                    log("params")
                    filter(Regex("foo")) // requires LocalVariables or -parameters
                    entity(param)
                }
            }
        }.execute { entityService ->
            fun verify(type: Entity.Type, label: String) {
                assertThat(entityService[type].map(::TestEntity))
                    .hasSize(1)
                    .first()
                    .isEqualTo(e(type, label))
            }

            verify(cls,    "MethodWithParam")
            verify(field,  "MethodWithParam.helloField")
            verify(method, "MethodWithParam::fn")
            verify(param,  "MethodWithParam::fn(foo: int)")
        }
    }

    @Test
    fun `conditional scope execution predicated on entity registration status`() {
        val foo = Entity.Type("foo")
        val bar = Entity.Type("bar")

        instrumenter {
            synthesize {
                entity(bar, type<Payload>())
            }

            scope("", ifExists, foo) {
                classesOf(bar) {
                    foo["bars"] = bar
                }
            }
            classes {
                scope("", ifExists, foo) {
                    foo["bars"] = bar
                }
                scope("", ifExists, foo) {
                    foo["bars"] = bar
                }
                fields {
                    scope("", ifExists, foo) {
                        foo["bars"] = bar
                    }
                }
                methods {
                    scope("", ifExists, foo) {
                        foo["bars"] = bar
                    }
                    parameters {
                        scope("", ifExists, foo) {
                            foo["bars"] = bar
                        }
                    }
                }
            }
        }.execute { }

        assertThrows<EntityNotFoundException> {
            classes {
                scope("", ifExistsNot, foo) {
                    foo["bars"] = bar
                }
            }.execute { }
        }
    }

    @Nested
    inner class JavaConstructsTests {

        @Test
        fun `recursive instantiations of sought types`() {
            val input = listOf(
                classNode(resource("/testdata/no-debug/ConstructB.class")),
                classNode(resource("/testdata/no-debug/ConstructB\$Payload.class")),
                classNode(resource("/testdata/no-debug/Fn.class")),
            )

            val payload = Entity.Type("payload")
            val foo = Entity.Type("foo")

            classes {
                scope("payload") {
                    filter(Regex("Payload\$"))
                    entity(payload)
                    log("payload")
                }

                filter(Regex("ConstructB\$"))
                methods {
                    filter(Regex("foo\$"))
                    entity(foo)
                    log("foo")

                    instantiationsOf(payload) {
                        log("payload instantiated")
                        foo["sends"] = payload
                    }
                }
            }.execute(input) { es ->
                assertThat(es[foo]).hasSize(1)
                assertThat(es[payload]).hasSize(1)

                val entity = es[foo].values.first()
                assertThat(entity.children("sends"))
                    .hasSize(1)
                    .first()
                    .isEqualTo(es[payload].values.first())
            }
        }

        @Test
        fun `read parameters when no debugging info is present`() {
            readParameters("/testdata/no-debug/ConstructA.class",
                "ConstructA::foo(string0: String)",
                "ConstructA::foo(list1: List)")
        }

        @Test
        fun `read parameters from LocalVariables`() {
            readParameters("/testdata/vars/ConstructA.class",
                "ConstructA::foo(aString: String)",
                "ConstructA::foo(listOfString: List)")
        }

        @Test
        fun `read parameters from parameter reflection data`() {
            readParameters("/testdata/params/ConstructA.class",
                "ConstructA::foo(aString: String)",
                "ConstructA::foo(listOfString: List)")
        }

        private fun readParameters(classResource: String, vararg expected: String) {
            val param = Entity.Type("param")
            val input = listOf(classNode(resource(classResource)))

            classes {
                methods {
                    parameters {
                        entity(param)
                    }
                }
            }.execute(input) { entityService ->
                assertThat(entityService[param].map(::TestEntity))
                    .hasSize(expected.size)
                    .containsAll(
                        expected.map { e(param, it) }
                    )
            }
        }
    }

    @Nested
    inner class NegativeTests {
        @Test
        fun `element must not belong to multiple entities`() {
            // entities of the same type can however be assigned
            // multiple times to the same element
            classes {
                entity(Entity.Type("a"))
                entity(Entity.Type("a"))
            }.execute { }

            assertThrows<UniqueElementPerEntityViolation> {
                classes {
                    entity(Entity.Type("a"))
                    entity(Entity.Type("b"))
                }.execute { }
            }
        }

        @Test
        fun `entity relations must point to defined entities`() {
            val foo = Entity.Type("foo")
            val bar = Entity.Type("bar")

            assertThrows<EntityNotFoundException> {
                classes {
                    foo["bars"] = bar
                }.execute { }
            }

            assertThrows<EntityNotFoundException> {
                classes {
                    entity(foo)
                    foo["bars"] = bar
                }.execute { }
            }

            assertThrows<EntityNotFoundException> {
                classes {
                    entity(bar)
                    foo["bars"] = bar
                }.execute { }
            }
        }
    }

    private fun Action<Unit, Unit>.execute(
        cns: List<ClassNode> = allCns,
        block: (EntityService) -> Unit
    ) {
        PipelineProcessor(cns).execute(this, false, false).let { pr -> pr.entityService }
    }
}

fun Iterable<Entity>.prettyPrint(): List<String> {
    return map { e -> "Entity(${e.label}, type=${e.type})" }
        .joinToString("\n")
        .lines()
}