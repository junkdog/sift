package sift.core.dsl

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.objectweb.asm.tree.ClassNode
import org.reflections.Reflections
import sift.core.*
import sift.core.EntityNotFoundException
import sift.core.UniqueElementPerEntityViolation
import sift.core.api.*
import sift.core.api.Context
import sift.core.api.Modifiers.*
import sift.core.dsl.ScopeEntityPredicate.ifExists
import sift.core.dsl.ScopeEntityPredicate.ifExistsNot
import sift.core.api.testdata.set1.*
import sift.core.api.testdata.set2.*
import sift.core.api.testdata.set3.InlineMarker
import sift.core.asm.classNode
import sift.core.asm.type
import sift.core.entity.Entity
import sift.core.entity.EntityService
import sift.core.template.toTree
import sift.core.tree.debugTree
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

    @Test
    fun `explode generic parameter type from class signature`() {
        val payload = Entity.Type("payload")
        val cns = listOf(classNode(ArrayListOfPayload::class))


        classes {
            superclass {
                typeArgument(1) {// filter n Payload
                    explodeType(synthesize = false) {
                        entity(payload)
                    }
                }
            }
        }.expecting(cns) { es ->
            val entities = es[payload].values.toList()
            assertThat(entities).hasSize(0)
        }

        classes {
            superclass {
                typeArgument(1) { // filter n Payload
                    explodeType(synthesize = true) {
                        logCount("entity")
                        entity(payload)
                    }
                }
            }
        }.expecting(cns) { es ->
            val entities = es[payload].values.toList()
            assertThat(entities).hasSize(1)
        }
    }

    @Test
    fun `exploring interfaces`() {
        val cns = listOf(
            classNode(Interfaces::class),
            classNode(Interfaces.B::class),
            classNode(Interfaces.C::class),
            classNode(Interfaces.D::class),
            classNode(Interfaces.E::class),

            classNode(Interfaces.ImplAx::class),
            classNode(Interfaces.ImplBx::class),
            classNode(Interfaces.ImplCx::class),
            classNode(Interfaces.ImplDx::class),
        )

        val e = Entity.Type("interfaces")

        fun EntityService.verify(
            matched: List<String>,
        ) {
            val entities = this[e].values.toList()
            assertThat(entities).hasSize(matched.size)
            assertThat(entities.map(Entity::label))
                .containsAll(matched.map { "Interfaces.$it" })
        }

        classes {
            filter(Regex("D\$"))
            interfaces(recursive = true) {
                entity(e)
            }
        }.expecting(cns) { es ->
            // A is not synthesized
            es.verify(listOf("B", "C"))
        }

        classes {
            filter(Regex("D\$"))
            interfaces(recursive = true, synthesize = true) {
                entity(e)
            }
        }.expecting(cns) { es ->
            es.verify(listOf("A", "B", "C"))
        }

        classes {
            filter(Regex("D\$"))
            interfaces {
                entity(e)
            }
        }.expecting(cns) { es ->
            es.verify(listOf("C"))
        }

        classes {
            filter(Regex("ImplDx\$"))
            interfaces {
                entity(e)
            }
        }.expecting(cns) { es ->
            es.verify(listOf("C", "D"))
        }

        classes {
            filter(Regex("ImplDx\$"))
            interfaces(recursive = true) {
                entity(e)
            }
        }.expecting(cns) { es ->
            es.verify(listOf("B", "C", "D"))
        }

        classes {
            filter(Regex("ImplDx\$"))
            interfaces(recursive = true, synthesize = true) {
                entity(e)
            }
        }.expecting(cns) { es ->
            // A is not synthesized
            es.verify(listOf("A", "B", "C", "D"))
        }
    }

    @Test
    fun `filter parameter and fields by type`() {
        val root = Entity.Type("class")
        val f = Entity.Type("field")
        val p = Entity.Type("param")

        classes {
            entity(root)
            fields {
                filterType(type<String>())
                entity(f)
                root["fields"] = f
            }

            methods {
                filter("fn")
                parameters {
                    filterType(type<String>())
                    entity(p)
                    root["params"] = p
                }
            }
        }.expecting(listOf(classNode(MethodWithParam::class)), root, """
            ── class
               └─ MethodWithParam
                  ├─ MethodWithParam.barField
                  └─ MethodWithParam::fn(bar: String)
            """
        )
    }

    @Test
    fun `explode Payload in List field and associate property from the main class`() {

        val payload = Entity.Type("payload")

        classes {
            filter("FieldClass")
            fields {
                signature {
                    typeArguments {
                        explodeType(synthesize = true) {
                            entity(payload, label("field-owner: \${field-owner}"))
                        }
                    }
                }
            }
            property(payload, "field-owner", readName())
        }.expecting(listOf(classNode(FieldClass::class)), payload, """
            ── payload
               └─ field-owner: FieldClass
            """
        )
    }

    @Test
    fun `explode generic parameter type of field`() {
        val cns = listOf(classNode(ClassWithGenericElements::class))

        val payload = Entity.Type("payload")

        classes {
            fields {
                signature {
                    typeArguments {
                        filter(Regex("Payload"))
                        explodeType(synthesize = true) {
                            entity(payload)
                        }
                    }
                }
            }
        }.expecting(cns, payload, """
            ── payload
               └─ Payload
            """
        )
    }

    @Test
    fun `register enum values as entities and find usages`() {
        val cns = listOf(classNode(Bob::class), classNode(Bobber::class))

        val bobber = Entity.Type("bob")
        val bobEnum = Entity.Type("enum")

        val expectedTree = """
            ── bob
               ├─ Bobber::a
               │  └─ Bob.A
               ├─ Bobber::b
               │  └─ Bob.A
               └─ Bobber::c
                  ├─ Bob.A
                  └─ Bob.B
            """

        // enums {} to register enum values as entities
        classes {
            scope("enum registration") {
                filter("Bob")
                enums { // faster
                    entity(bobEnum)
                }
            }

            scope("enum usage") {
                filter("Bobber")
                methods {
                    filter("<init>", invert = true)
                    entity(bobber)

                    bobber["references"] = bobEnum.fieldAccess
                }
            }
        }.expecting(cns, bobber, expectedTree)

        // identify enums manually; functionally equivalent, but slower
        classes {
            scope("enum registration") {
                filter("Bob")
                fields {
                    filter(acc_static, acc_final, acc_enum)
                    entity(bobEnum)
                }
            }

            scope("enum usage") {
                filter("Bobber")
                methods {
                    filter("<init>", invert = true)
                    entity(bobber)

                    bobber["references"] = bobEnum.fieldAccess
                }
            }
        }.expecting(cns, bobber, expectedTree)
    }

    @Test
    fun `wire entities with fieldAccess when type is class`() {
        val cns = listOf(classNode(Dob::class), classNode(Dibbler::class))
        val dob = Entity.Type("dob")
        val dibbler = Entity.Type("dibbler")

        // fixme: poor test, plus not working; see sift self template
        template {
            classes {

                scope("dibbler") {
                    filter("Dibbler")
                    methods {
                        filter("<init>", invert = true)
                        entity(dibbler)
                    }
                }

                scope("dob") {
                    filter("Dob")
                    fields {
                        filter("INSTANCE", invert = true)
                        explodeType(synthesize = true) {
                            entity(dob)
                        }
                    }
                }

            }

            dibbler["references"] = dob.fieldAccess
        }.expecting(cns, dibbler, """
            ── dibbler
               ├─ Dibbler::a
               │  └─ String
               ├─ Dibbler::b
               │  └─ String
               └─ Dibbler::c
                  └─ String
        """)
    }

    @Test
    fun `internal scope in signature scope`() {
        val cns = listOf(classNode(ClassExtendingMapOfOmgPayload::class))

        val omg = Entity.Type("omg")
        val payload = Entity.Type("payload")

        classes {
            superclass {
                typeArguments {
                    scope("omg") {
                        filter(Regex("Omg\$"))
                        explodeType(synthesize = true) {
                            entity(omg)
                        }
                    }
                    scope("payload") {
                        filter(Regex("Payload\$"))
                        explodeType(synthesize = true) {
                            entity(payload)
                        }
                    }
                }
            }
        }.expecting(cns) { es ->
            assertThat(es[omg].values).hasSize(1)
            assertThat(es[payload].values).hasSize(1)
        }
    }

    @Test
    fun `explode nested generic parameter types from method return`() {
        val cns = listOf(classNode(ClassWithGenericElements::class))
        val payload = Entity.Type("payload")

        fun validate(template: Action<Unit, Unit>) = template.expecting(cns, payload, """
            ── payload
               └─ Payload
            """
        )

        // fun complexReturn(): Map<String, List<Pair<Payload, Int>>>
        val template = classes {
            methods {
                returns {
                    explodeTypeT("Map<String, List<Pair<T, _>>>", synthesize = true) {
                        entity(payload)
                    }
                }
            }
        }.also(::validate)

        val expected = classes {
            methods {
                returns {
                    filter(Regex("^.+\\.Map\$"))
                    typeArgument(1) {                  //  List<Pair<Payload, Int>>
                        filter(Regex("^.+\\.List\$"))
                        typeArgument(0) {              // Pair<Payload, Int>
                            filter(Regex("^.+\\.Pair\$"))
                            typeArgument(0) {          // Payload
                                explodeType(synthesize = true) {
                                    entity(payload)
                                }
                            }
                        }
                    }
                }
            }
        }.also(::validate)

        assertThat(template.debugTree())
            .isEqualTo(expected.debugTree())

        // making sure unspecified types are ok too
        classes {
            methods {
                returns {
                    explodeTypeT("_<_, _<_<T, _>>>", synthesize = true) {
                        entity(payload)
                    }
                }
            }
        }.also(::validate)
    }

    @Test
    fun `generic method parameters`() {
        val cns = listOf(classNode(ClassWithGenericMethodParameters::class))
        val payload = Entity.Type("payload")
        val method = Entity.Type("method")

        classes {
            methods {
                parameters {
                    parameter(0)
                    outerScope("register method") {
                        entity(method, label("\${name}"), property("name", readName()))
                    }

                    signature {
                        typeArguments {
                            filter(Regex("Payload"))
                            explodeType(synthesize = true) {
                                entity(payload)
                                method["payload"] = payload
                            }
                        }
                    }
                }
            }
        }.expecting(cns, method, """
            ── method
               ├─ complexParameters
               └─ payloads
                  └─ Payload
            """
        )
    }

    @Test @Disabled
    fun `read class and enum from annotation`() {
        TODO()
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
                property("double", readAnnotation(AnnoPrimitives::double))
            )

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
                    property("double", readAnnotation(AnnoPrimitives::double))
                )

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
                        property("double", readAnnotation(AnnoPrimitives::double))
                    )

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
                    property("double", readAnnotation(AnnoPrimitives::double))
                )
            }
        }.expecting { entityService ->
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
    fun `entity assignment via outerScope`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        classes {
            methods {
                annotatedBy<Endpoint>()
                entity(endpoint)

                outerScope("register controller class") {
                    log("iterating set of classes with @Endpoint methods")
                    entity(controller)
                    controller["endpoints"] = endpoint
                }
            }
        }.expecting(allCns, controller, """
            ── controller
               └─ SomeController
                  ├─ SomeController::create
                  └─ SomeController::delete
            """
        )
    }

    @Test
    fun `read field name and explode synthesized field type`() {
        val field = Entity.Type("field")
        val foo = Entity.Type("foo")

        val cns = listOf(
            classNode<ClassWithFields>(),
        )

        // note that kotlin properties are not necessarily backed by actual fields
        classes {
            filter(Regex("ClassWithFields\$"))
            fields {
                entity(field, label("\${name}"),
                    property("name", readName()))

                explodeType(true) {
                    entity(foo)
                }
            }
        }.expecting(cns) { es ->
            assertThat(es[foo].values.map(Entity::label))
                .hasSize(1)
                .first()
                .isEqualTo("ClassWithFields.Foo")
            assertThat(es[field].values.map(Entity::label))
                .hasSize(1)
                .first()
                .isEqualTo("field")
        }
    }

    @Test
    fun `scope to methods of registered entities`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        template {
            classes {
                methods {
                    annotatedBy<Endpoint>()
                    entity(endpoint)
                }
            }

            methodsOf(endpoint) {
                outerScope("register controller") {
                    entity(controller)
                    controller["endpoints"] = endpoint
                }
            }
        }.expecting(allCns, controller, """
            ── controller
               └─ SomeController
                  ├─ SomeController::create
                  └─ SomeController::delete
            """
        )
    }

    @Test
    fun `update entity properties from sub-scope`() {
        val cns: List<ClassNode> = listOf(
            classNode<MethodsWithTypes>(),
        )

        val et = Entity.Type("e")

        classes {
            log("classes")
            entity(et, label("\${a-parameter-type}"))
            methods {
                filter(Regex("mixedTypes"))
                parameters {
                    parameter(1)
                    property(et, "a-parameter-type", readType())
                }
            }
        }.expecting(cns, et, """
            ── e
               └─ java.lang.String
            """
        )
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
        }.expecting(cns, paramType, """
            ── param-type
               ├─ MethodsWithTypes.Bar
               └─ MethodsWithTypes.Foo
            """
        )

        classes {
            methods {
                filter(Regex("mixedTypes"))
                parameters {
                    explodeType {
                        entity(paramType)
                    }
                }
            }
        }.expecting(cns, paramType, """
            ── param-type
               └─ MethodsWithTypes.Foo
            """
        )

        classes {
            methods {
                filter(Regex("noPresentTypes"))
                parameters {
                    explodeType {
                        entity(paramType)
                    }
                }
            }
        }.expecting(cns) { es ->
            assertTrue(paramType !in es)
        }
    }

    @Test
    fun `recursively scan for instantiations`() {
        val cns = listOf(
            classNode<SimpleInstantiations>(),
            classNode<SimpleInstantiations.Yolo>(),
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
                filter(Regex("<(clinit|init)>"), invert = true)
                entity(method)
                instantiationsOf(payload) {
                    logCount("instantiations")
                    method["instantiates"] = payload
                    payload["backtrack"] = method
                }
            }
        }.expecting(cns, method,
            """
            ── method
               ├─ Instantiations2::a
               │  └─ Payload
               ├─ Instantiations2::b
               │  └─ Payload
               ├─ Instantiations2::c
               │  └─ Payload
               ├─ Instantiations2::notCalled
               ├─ SimpleInstantiations.Yolo::hmm
               │  └─ Payload
               ├─ SimpleInstantiations::caseA
               │  └─ Payload
               └─ SimpleInstantiations::caseB
                  └─ Payload
            """.trimIndent())
    }

    @Test @Disabled
    fun `read annotation from generic parameter`() {
        TODO()
    }

    @Test @Disabled
    fun `read annotation from method parameter`() {
        TODO()
    }

    @Test
    fun `register generic type from method parameters and fields`() {
        // interface RepoT<T>
        // abstract class AbstractRepoT<T>
        //
        // class GenericRepos {
        //     var iRepoInt: RepoT<Int> = object : RepoT<Int> {}
        //     var aRepoInt: RepoT<Int> = object : RepoT<Int> {}
        //
        //     fun aRepoString(repoT: AbstractRepoT<String>) = Unit
        //     fun iRepoString(repoT: RepoT<String>) = Unit
        //}

        val cns: List<ClassNode> = listOf(
            classNode(AbstractRepoT::class),
            classNode(GenericRepos::class),
            classNode(RepoT::class),
        )

        val et = Entity.Type("e")

        classes {
             fields {
                filter(Regex("RepoInt"))
                signature {
                    entity(et)
                }
            }

            methods {
                filter(Regex("RepoString"))
                parameters {
                    parameter(0)
                    signature {
                        entity(et)
                    }
                }
            }
        }.expecting(cns, et,
            """
            ── e
               ├─ AbstractRepoT<String>
               ├─ RepoT<Integer>
               └─ RepoT<String>
            """
        )
    }

    @Test @Disabled
    fun `resolve interface generic type inherited from abstract class`() {
        // interface RepoT<T>
        // abstract class AbstractRepoT<T>
        //
        // class GenericRepos {
        //     var iRepoInt: RepoT<Int> = object : RepoT<Int> {}
        //     var aRepoInt: RepoT<Int> = object : RepoT<Int> {}
        //
        //     fun aRepoString(repoT: AbstractRepoT<String>) = Unit
        //     fun iRepoString(repoT: RepoT<String>) = Unit
        //}

        TODO()

        val cns: List<ClassNode> = listOf(
            classNode(AbstractRepoT::class),
            classNode(RepoT::class),
        )

        val et = Entity.Type("e")

        classes {
            fields {
                filter(Regex("RepoInt"))
                signature {
                    entity(et)
                }
            }
        }.expecting(cns, et, """
            ── e
               └─ GenericRepos
            """
        )
    }

    @Test @Disabled
    fun `resolve invocations on generic interfaces`() {
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
                property("name", readName())
            )
        }.expecting(cns, et, """
            ── class
               └─ CLS MethodsWithTypes
            """
        )
    }

    @Test
    fun `handle kotlin functions with noinline and crossinline parameters`() {
        val cns: List<ClassNode> =  Reflections("sift.core.api.testdata.set3")
            .getTypesAnnotatedWith(Metadata::class.java)
            .map(::classNode)

        val hof = Entity.Type("hof")
        val foo = Entity.Type("foo")

        classes {

            scope("register Foo") {
                filter(Regex("Foo\$"))
                entity(foo)
            }

            methods {
                annotatedBy<InlineMarker>()
                entity(hof)

                hof["foo"] = foo.instantiations
            }
        }.expecting(cns) { es ->
            assertThat(es[foo]).hasSize(1)
            assertThat(es[hof]).hasSize(5)

            val (a, b, c, d, e) = es[hof].values.toList()
            assertThat(a.children["foo"]).isNotEmpty()
            assertThat(b.children["foo"]).isNotEmpty()
            assertThat(c.children["foo"]).isNotEmpty()
            assertThat(d.children["foo"]).isNotEmpty()
            assertThat(e.children["foo"]).isNotEmpty()
        }
    }

    @Test
    fun `read outer class`() {
        val cns: List<ClassNode> =  Reflections("sift.core.api.testdata.set3")
            .getTypesAnnotatedWith(Metadata::class.java)
            .map(::classNode)

        val foo = Entity.Type("foo")

        classes {

            filter(Regex("Foo\$"))
            entity(foo, label("\${outer}[\${name}]"),
                property("name", readName(shorten = true)))

            outerScope("read outer class name") {
                property(foo, "outer", readName())
            }


        }.expecting(cns) { es ->
            assertThat(es[foo].values.map(Entity::label))
                .containsExactly("KotlinInliningIntrospection[Foo]")
        }
    }

    @Test
    fun `filter classes`() {
        val controller = Entity.Type("controller")

        classes {
            filter(Regex("^sift\\.core\\.api\\.testdata"))

            annotatedBy<RestController>()
            entity(controller)
        }.expecting(allCns, controller, """
            ── controller
               └─ SomeController
            """
        )
    }

    @Test
    fun `read endpoint and construct label`() {
        val endpoint = Entity.Type("endpoint")

        template {
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
        }.expecting(allCns, endpoint, """
            ── endpoint
               ├─ DELETE /bar
               └─ POST /foo
            """
        )
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
        template {

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
        }.expecting(cns, ::validate)

        // inline synthesize
        template {
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
        }.expecting(cns, ::validate)
    }

    @Test
    fun `find implementations of generic interfaces`() {
        val cns = listOf(
            classNode<GenericInterfaceImpl>(),
            classNode<HandlerOfFns>(),
        )

        val impl = Entity.Type("implementer")

        classes {
            implements(type("sift.core.api.testdata.set2.GenericInterface"))
            entity(impl)
        }.expecting(cns, impl, """
            ── implementer
               └─ GenericInterfaceImpl
            """
        )
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
        }.expecting(cns, base, """
            ── base
               ├─ Interfaces.ImplementorB
               ├─ Interfaces.ImplementorC
               └─ Interfaces.ImplementorD
            """
        )
    }

    @Test  // <-- annotation(/decorator)
    fun `filter on generic superclass`() { // fun `function name`() {} ; `function name`()
        val cns = listOf(
            classNode<GenericClass<*>>(), //  abstract class GenericClass<T>
            classNode<ConcreteClass1>(),  //  class ConcreteClass1 : GenericClass<String>()
            classNode<ConcreteClass2>(),  //  class ConcreteClass2 : GenericClass<Float>()
        )

        val a = Entity.Type("a")
        val b = Entity.Type("b")

        classes {
            scope("a") {
                // TODO: shorten to `type("GenericClass<String>")`
                implements(type("sift.core.api.testdata.set2.GenericClass<java.lang.String>"))
                entity(a, label("A \${name}"),
                    property("name", readName(shorten = true)))
            }

            scope("b") {
                implements(type("sift.core.api.testdata.set2.GenericClass<java.lang.Float>"))
                entity(b, label("B \${name}"),
                    property("name", readName(shorten = true)))
            }
        }.expecting(cns, listOf(a, b),
            """
            ── a + b
               ├─ A ConcreteClass1
               └─ B ConcreteClass2
            """
        )
    }

    @Test
    fun `filter on implementation of generic interface`() {
        //  class GenericInterfaceImpl : GenericInterface<String, Int>
        //  class GenericInterfaceImpl2 : GenericInterface<String, Float>
        //  class GenericInterfaceImpl3 : GenericInterface<String, GenericInterface<Boolean, String>>

        val cns = listOf(
            classNode<GenericInterface<*, *>>(),
            classNode<GenericInterfaceImpl>(),
            classNode<GenericInterfaceImpl2>(),
            classNode<GenericInterfaceImpl3>(),
        )

        val a = Entity.Type("a")
        val b = Entity.Type("b")

        val genericInterface = "sift.core.api.testdata.set2.GenericInterface"

        classes {
            scope("a") {
                // TODO: make this work with "GenericInterface<String, Integer>"
                implements(type("$genericInterface<java.lang.String, java.lang.Integer>"))
                entity(a, label("A \${name}"),
                    property("name", readName()))
            }
            scope("b") {
                implements(type("$genericInterface<java.lang.String, java.lang.Float>"))
                entity(b, label("B \${name}"),
                    property("name", readName()))
            }
        }.expecting(cns, listOf(a, b),
            """
            ── a + b
               ├─ A GenericInterfaceImpl
               └─ B GenericInterfaceImpl2
            """
        )
    }

    @Test
    fun `synthesize missing inherited methods`() {
        val cns = listOf(
            classNode<RepoImpl>(),
            classNode<RepoClient>(),
        )

        val repo = Entity.Type("repo")
        val fn = Entity.Type("repo-method")

        template {
            classes {
                log("pre-repository")
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
        }.expecting(cns, repo, """
            ── repo
               └─ RepoImpl
                  ├─ RepoImpl::a
                  ├─ RepoImpl::b
                  ├─ RepoImpl::c
                  └─ RepoImpl::d
            """
        )
    }

    @Test
    fun `child assignment using dot-invocations`() {
        val cns = listOf(
            classNode<HandlerFn>(),
            classNode<HandlerOfFns>(),
        )

        val method = Entity.Type("handler")

        classes {
            methods {
                filter(Regex("HandlerOfFns\$"))
                entity(method)

                method["invokes"] = method.invocations
            }
        }.expecting(cns, method,
            """
            ── handler
               ├─ HandlerOfFns::<init>
               ├─ HandlerOfFns::boo
               ├─ HandlerOfFns::dummy
               ├─ HandlerOfFns::invoker
               │  ├─ HandlerOfFns::boo
               │  └─ HandlerOfFns::on
               └─ HandlerOfFns::on
            """
        )
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
        }.expecting(cns, handler,
            """
            ── handler
               ├─ HandlerOfFns::boo
               │  └─ Payload
               └─ HandlerOfFns::on
            """
        )
    }

    @Test
    fun `child assignment using dot-invocations-by`() {
        val cns = listOf(
            classNode<Payload>(),
            classNode<SomeFactory>(),
            classNode<HandlerOfFns>(),
        )

        val method = Entity.Type("handler")
        val factory = Entity.Type("factory")

        classes {
            scope("factory") {
                filter(Regex("SomeFactory"))
                methods {
                    filter(Regex("create"))
                    entity(factory)
                }
            }
            methods {
                filter(Regex("HandlerOfFns\$"))
                entity(method)

                factory.invocations["invocations-by"] = method
            }
        }.expecting(cns) { es ->
            val methods = es[method].values.toList()
            assertThat(methods).hasSize(5)

            fun er(s: String): Entity = methods.first { s in it.label }

            assertThat(es[factory].values.first().children("invocations-by"))
                .hasSize(2)
                .containsExactly(
                    er("HandlerOfFns::boo"),
                    er("HandlerOfFns::invoker"),
                )
        }
    }

    @Test
    fun `update properties on multiple entity types with elementsOf`() {
        val cns = listOf(
            classNode<SomethingAnnotated>(),
        )

        val cls = Entity.Type("class")
        val method = Entity.Type("method")
        val field = Entity.Type("field")
        val param = Entity.Type("param")

        template {
            classes {
                entity(cls, label("\${v}"))

                fields {
                    filter(Regex("otherField"))
                    entity(field, label("\${v}"))
                }

                methods {
                    filter(Regex("foo"))
                    entity(method, label("\${v}"))

                    parameters {
                        parameter(0)
                        entity(param, label("\${v}"))
                    }
                }
            }

            listOf(cls, method, field, param).forEach { e ->
                elementsOf(e) {
                    property(e, "v", withValue(e.id))
                }
            }

        }.expecting(cns) { es ->
            fun validate(type: Entity.Type) {
                val entities = es[type].values.toList()
                assertThat(entities)
                    .hasSize(1)

                assertThat(entities.first().properties)
                    .containsKey("v")
                    .containsEntry("v", mutableListOf(type.id))
            }

            validate(cls)
            validate(method)
            validate(field)
            validate(param)
        }
    }

    @Test
    fun `child assignment using dot-instantiations-by`() {
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

                data.instantiations["instantiations-by"] = handler
            }
        }.expecting(cns, data, """
            ── data
               └─ Payload
                  └─ HandlerOfFns::boo
            """
        )
    }

    @Test
    fun `iterate registered class entities`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        template {
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
                        property("path", readAnnotation(Endpoint::path))
                    )
                }
            }
        }.expecting(allCns, endpoint, """
            ── endpoint
               ├─ DELETE /bar
               └─ POST /foo
            """
        )
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
                    invokes(handler)
                    entity(invoker)
                    invoker["invokes"] = handler
                    handler["invoked-by"] = invoker
                }
            }
        }.expecting(cns) { es ->
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
                    property("path", readAnnotation(Endpoint::path))
                )

                controller["endpoints"] = endpoint
            }
        }.expecting(allCns, controller, """
            ── controller
               └─ SomeController
                  ├─ DELETE /bar
                  └─ POST /foo
            """
        )
    }

    @Test
    fun `mix and match templates`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")

        val templateA = classes {
            filter(Regex("^sift\\.core\\.api\\.testdata"))
            annotatedBy<RestController>()
            entity(controller)
        }

        val templateB = template {
            methodsOf(controller) {
                annotatedBy<Endpoint>()
                entity(endpoint, label("\${http-method} \${path}"),
                    property("http-method", readAnnotation(Endpoint::method)),
                    property("path", readAnnotation(Endpoint::path)),
                )

                controller["endpoints"] = endpoint
            }
        }

        template {
            include(templateA)
            include(templateB)
        }.expecting(allCns, controller, """
            ── controller
               └─ SomeController
                  ├─ DELETE /bar
                  └─ POST /foo
            """
        )
    }

    @Test
    fun `read annotations from forked subsets using scope`() {
        val controller = Entity.Type("controller")
        val endpoint = Entity.Type("endpoint")
        val query = Entity.Type("query")

        classes {
            filter(Regex("^sift\\.core\\.api\\.testdata"))
            annotatedBy<RestController>()
            entity(controller)

            methods {
                scope("scan endpoints") {
                    annotatedBy<Endpoint>()
                    entity(endpoint, label("\${http-method} \${path}"),
                        property("http-method", readAnnotation(Endpoint::method)),
                        property("path", readAnnotation(Endpoint::path))
                    )

                    log("Endpoints")
                    controller["endpoints"] = endpoint
                }
                scope("scan queries ") {
                    annotatedBy<Query>()
                    entity(query)
                    controller["queries"] = query
                }
            }
        }.expecting(allCns, controller, """
            ── controller
               └─ SomeController
                  ├─ DELETE /bar
                  ├─ POST /foo
                  └─ SomeController::query
            """
        )
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
        }.expecting { entityService ->
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

        template {
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
        }.expecting { }

        assertThrows<EntityNotFoundException> {
            classes {
                scope("", ifExistsNot, foo) {
                    foo["bars"] = bar
                }
            }.expecting { }
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
            }.expecting(input) { es ->
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
            }.expecting(input) { entityService ->
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
    fun `entity types can only belong to one type of element`() {
        val cns: List<ClassNode> = listOf(
            classNode<MethodsWithTypes>(),
        )

        val et = Entity.Type("class")

        // works
        classes {
            // entity(et, property("type", readType()))
            methods {
                filter(Regex("mixedTypes"))
                parameters {
                    entity(et, property("type", readType()))
                }
            }
        }.expecting(cns) {}

        assertThrows<IllegalEntityAssignmentException> {
            classes {
                entity(et, property("type", readType()))
                methods {
                    filter(Regex("mixedTypes"))
                    parameters {
                        entity(et, property("type", readType()))
                    }
                }
            }.expecting(cns) {}
        }
    }

        @Test
        fun `element must not belong to multiple entities`() {
            // entities of the same type can however be assigned
            // multiple times to the same element
            classes {
                entity(Entity.Type("a"))
                entity(Entity.Type("a"))
            }.expecting { }

            assertThrows<UniqueElementPerEntityViolation> {
                classes {
                    entity(Entity.Type("a"))
                    entity(Entity.Type("b"))
                }.expecting { }
            }
        }

        @Test
        fun `entity relations must point to defined entities`() {
            val foo = Entity.Type("foo")
            val bar = Entity.Type("bar")

            assertThrows<EntityNotFoundException> {
                classes {
                    foo["bars"] = bar
                }.expecting { }
            }

            assertThrows<EntityNotFoundException> {
                classes {
                    entity(foo)
                    foo["bars"] = bar
                }.expecting { }
            }

            assertThrows<EntityNotFoundException> {
                classes {
                    entity(bar)
                    foo["bars"] = bar
                }.expecting { }
            }
        }
    }

    private fun Action<Unit, Unit>.expecting(
        cns: List<ClassNode> = allCns,
        block: (EntityService) -> Unit
    ) {
        TemplateProcessor(cns)
            .process(this, false, Context::debugTrails)
            .entityService
            .also(block)
    }

    private fun Action<Unit, Unit>.expecting(
        cns: List<ClassNode> = allCns,
        root: Entity.Type,
        expectTree: String
    ) {
        expecting(cns, listOf(root), expectTree)
    }

    private fun Action<Unit, Unit>.expecting(
        cns: List<ClassNode> = allCns,
        root: List<Entity.Type>,
        expectTree: String
    ) {
        val noAnsi = Terminal(ansiLevel = AnsiLevel.NONE)
        expecting(cns) { es ->
            assertThat(es.toTree(root).let(noAnsi::render))
                .isEqualTo(expectTree.trimIndent() + "\n")
        }
    }
}

fun Iterable<Entity>.prettyPrint(): List<String> {
    return map { e -> "Entity(${e.label}, type=${e.type})" }
        .joinToString("\n")
        .lines()
}

fun EntityService.toTree(roots: List<Entity.Type>): String {
    return SystemModel(this).toTree(roots).toString()
}

private class FieldClass {
    val payloads: List<PayLoad> = listOf()
}

private class PayLoad
