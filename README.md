# sift

Sift is a tool that allows you to model and analyze the design of systems from Java class
files. With Sift, you can build, query, and diff system models using the command-line interface.

## Features
- CLI tool for building, querying, and [diff-ing][diff] system models from parsed .class files.
- System Models consist of Entities and are produced by Instrumenter Pipelines.
- Instrumenter Pipelines provide knowledge about technology stacks for static bytecode analysis.
- Declarative DSL for user-defined pipelines.
- JSON serialization of pipelines for easy reuse and sharing.
- Inline [rendering of system representations][graphviz] using Graphviz.

![sift spring-boot axon framework][sift-render]

_Spring-Boot with Axon Framework [instrumenter][spring-axon] in action; filtering on shipped and confirmed orders
in https://github.com/eugenp/tutorials/tree/master/axon._

 [spring-axon]: instrumenters/spring-boot-axon-cqrs/src/main/kotlin/sift/instrumenter/sbacqrs/SpringBootAxonCqrsInstrumenter.kt#L150:L220
 [diff]: docs/images/sift-spring-axon-diff.png
 [graphviz]: docs/images/sift-spring-axon-render.png
 [sift-render]: docs/images/sift-render-s.png

## CLI options

```
Usage: sift [OPTIONS]

  A tool to model and analyze the design of systems from bytecode.

Options:
  -f, --class-dir PATH                        jar or directory with classes
  -l, --list-instrumenters                    print all instrumenters detected on the current classpath
  -i, --instrumenter INSTRUMENTER             the instrumenter pipeline performing the scan
  -R, --render                                render entities in graphviz's DOT language
  -X, --dump-system-model                     print all entities along with their properties and metadata
  --profile                                   print execution times and input/output for the executed
                                              pipeline
  -T, --tree-root ENTITY-TYPE                 tree built around requested entity type
  -t, --list-entity-types                     lists entity types defined by instrumenter
  -L, --max-depth INT                         Max display depth of the tree
  -F, --filter REGEX                          filters nodes by label. can occur multiple times
  -S, --filter-context REGEX                  filters nodes by label, while also including sibling nodes.
                                              can occur multiple times
  -e, --exclude REGEX                         excludes nodes when label matches REGEX; can occur multiple
                                              times
  -E, --exclude-type ENTITY-TYPE              excludes entity types from tree; can occur multiple times
  -s, --save FILE_JSON                        save the resulting system model as json; for later use by
                                              --diff or --load
  --load FILE_JSON                            load a previously saved system model
  -d, --diff FILE_JSON                        load a previously saved system model
  -a, --ansi [none|ansi16|ansi256|truecolor]  override automatically detected ANSI support
  --version                                   print version and release date
  --debug                                     prints log/logCount statements from the executed pipeline
  --generate-completion [bash|zsh|fish]
  -h, --help                                  Show this message and exit
```

## Entity and Entity Type

In a system model, entities are the individual components that make up the system, 
such as classes, methods, fields, and parameters. Each entity is uniquely identified
by one of these elements and cannot be associated with more than one entity.

Each entity is mapped to a specific type, which represents any notable part of the
system. For example, types can include REST controllers, HTTP endpoints, inbound/outbound
messages, RDS, and more.

```bash
$ sift --instrumenter spring-axon --list-entity-types target/classes
entity types of spring-axon
  1 aggregate
  2 aggregate-ctor
  1 aggregate-member
  6 command
  6 command-handler
  1 controller
 13 endpoint
  7 event
  7 event-handler
  7 event-sourcing-handler
  1 projection
  3 query
  4 query-handler
```
## Instrumenter pipelines

The system model describes the structure and relationships of entities within a system.
An entity is a unique object within the system, identified by a class, method, field, or
parameter. Each entity is mapped to a type, which represents a notable part of the system,
such as a REST controller or an inbound message.

Instrumenter Pipelines provide knowledge about a technology stack and/or project-specific
constructs. These pipelines are written in a declarative DSL and are used to produce the
system model from input classes. The DSL provides high-level abstractions for identifying
and interrelating entities from class structure or usage.

The code below shows a simple Instrumenter Pipeline that identifies REST controllers and
HTTP endpoints within a system and associates the two entities.

```kotlin
val controller = Entity.Type("controller")
val endpoint = Entity.Type("endpoint")

instrumenter {
    // iterate over all input classes
    classes {                                                      
        annotatedBy<RestController>() // filter classes 
        entity(controller)            // mark remaining as 'controller'  
        methods {                     // iterate all controller methods
            annotatedBy<Endpoint>()   // filter @Endpoint methods
            entity(endpoint)

            // associate controllers with their endpoints  
            controller["endpoints"] = endpoint
        }
    }
}
```
Input elements (classes, methods, parameters, and fields) are processed in batches, line-by-line.
The execution of an Instrumenter Pipeline can be visualized with the --profile option.

A typical Instrumenter Pipeline can be expressed in about 100 lines of code. Some pipelines,
such as those for [JPA][jpa] and [JDBI][jdbi], are notably shorter. User-defined pipelines
can include multiple existing pipelines to better describe the underlying system while also
keeping the resulting pipeline DSL concise.

 [jpa]: instrumenters/jpa/src/main/kotlin/sift/instrumenter/jpa/JpaInstrumenter.kt#L48:L73
 [jdbi]: instrumenters/jdbi/src/main/kotlin/sift/instrumenter/jdbi/Jdbi3Instrumenter.kt#L54:L67

![sift spring-boot axon framework demo](docs/images/sift-spring-axon-profile-pipeline.png)


## Building a native binary on linux using graalvm

If graalvm and native-image is installed, a native binary can be built with the `native-image`
maven profile: `mvn clean install -P native-image`. The resulting binary will be located in
`~/.local/share/sift/bin/sift`. `sift.zsh` and `sift.sh` first checks if the native binary
is available, otherwise it tries to run the jar.

The native binary is considerably faster than the jar, but it can cause issues if it needs
to deserialize a system model (via `--load` or `--diff`) or instrumenter pipeline containing
unknown types (e.g. from `withValue()`).


## Caveats and limitations
- no flow analysis making precise entity identification difficult for e.g. dispatcher-like 
  methods dealing with multiple entity types.
