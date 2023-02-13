# sift

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.onedaybeard.sift/sift/badge.png)](https://maven-badges.herokuapp.com/maven-central/net.onedaybeard.sift/sift/badge.png)

Sift is a tool that allows you to model and analyze the design of systems from Java class
files. With Sift, you can build, query, and diff system models using the command-line interface.

## Features
- CLI tool for building, querying, and [diff-ing][diff] system models from parsed .class files.
- System Models consist of Entities and are produced by Instrumenter Pipelines.
- System Model Templates provide knowledge about technology stacks for static bytecode analysis.
- Declarative DSL for user-defined templates.
- JSON serialization of templates for easy reuse and sharing.
- Inline rendering of system representations using Graphviz.

![sift spring-boot axon framework][sift-render]

_Spring-Boot with Axon Framework [template][spring-axon] in action; filtering on shipped and confirmed orders
in https://github.com/eugenp/tutorials/tree/master/axon. (Use [kitty](https://sw.kovidgoyal.net/kitty/) to render 
straight into the terminal.)_

 [spring-axon]: templates/spring-boot-axon-cqrs/src/main/kotlin/sift/template/sbacqrs/SpringBootAxonCqrsInstrumenter.kt#L150:L220
 [diff]: docs/images/sift-spring-axon-diff.png
 [graphviz]: docs/images/sift-spring-axon-render.png
 [sift-render]: docs/images/sift-render-s.png

## CLI options

```
Usage: sift [OPTIONS]

  A tool to model and analyze the design of systems from bytecode.

Entity tree options:
  -L, --max-depth INT             Max display depth of the tree.
  -F, --filter REGEX              Filters nodes by label. (repeatable)
  -S, --filter-context REGEX      Filters nodes by label, while also including sibling
                                  nodes. (repeatable)
  -e, --exclude REGEX             Excludes nodes when label matches REGEX. (repeatable)
  -E, --exclude-type ENTITY-TYPE  Excludes entity types from tree. (repeatable)
  -b, --tree-root ENTITY-TYPE     Tree built around requested entity type.

Visualization options:
  -R, --render                          Render entities with graphviz's DOT language.
  --edge-layout [spline|polyline|ortho]
                                        Sets the layout for the lines between nodes.
                                        (default: spline)

Serialization options:
  -s, --save FILE_JSON  Save the resulting system model as json.
  --load FILE_JSON      Load a previously saved system model.
  -d, --diff FILE_JSON  Diff view against a previously saved system model.

Options:
  -f, --class-dir PATH                  Jar or directory with classes.
  -l, --list-templates                  Print all templates detected on the current
                                        classpath.
  -t, --template TEMPLATE               The template producing the system model.
  -X, --dump-system-model               Print all entities along with their properties and
                                        metadata.
  --profile                             Print execution times and input/output for the
                                        executed template.
  -T, --list-entity-types               Lists entity types defined by template.
  -a, --ansi [none|ansi16|ansi256|truecolor]
                                        Override automatically detected ANSI support.
  --version                             Print version and release date.
  --debug                               Print log/logCount statements from the executed
                                        pipeline.
  --generate-completion [bash|zsh|fish]
  -h, --help                            Show this message and exit
```

## Entity and Entity Type

In a system model, entities are the individual components that make up the system, 
such as classes, methods, fields, and parameters. Each entity is uniquely identified
by one of these elements and cannot be associated with more than one entity.

Each entity is mapped to a specific type, which represents any notable part of the
system. For example, types can include REST controllers, HTTP endpoints, inbound/outbound
messages, RDS, and more.

```bash
$ sift --template spring-axon --list-entity-types target/classes
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
## System Model Templates

The system model describes the structure and relationships of entities within a system.
An entity is a unique object within the system, identified by a class, method, field, or
parameter.

System Model Templates describe how entities are identified within a given technology stack and/or project-specific
constructs. The templates are written in a declarative DSL and are used to produce the
system model from input classes. The DSL provides high-level abstractions for identifying
and interrelating entities from class structure or usage.

The code below shows a simple System Model Template that identifies REST controllers and
HTTP endpoints within a system and associates the two entities.

```kotlin
val controller = Entity.Type("controller")
val endpoint = Entity.Type("endpoint")

template {
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
The execution of a System Model Template can be introspected with the `--profile` option.

A typical template can be expressed in about 100 lines of code. Some templates,
such as those for [JPA][jpa] and [JDBI][jdbi], are notably shorter. User-defined templates
can include multiple existing templates to better describe the underlying system while also
keeping the resulting DSL concise.

 [jpa]: templates/jpa/src/main/kotlin/sift/template/jpa/JpaInstrumenter.kt#L48:L73
 [jdbi]: templates/jdbi/src/main/kotlin/sift/template/jdbi/Jdbi3Instrumenter.kt#L54:L67

![sift spring-boot axon framework demo](docs/images/sift-spring-axon-profile-pipeline.png)


## Building a native binary on linux using graalvm

If graalvm and native-image is installed, a native binary can be built with the `native-image`
maven profile: `mvn clean install -P native-image`. The resulting binary will be located in
`~/.local/share/sift/bin/sift`. `sift.zsh` and `sift.sh` first checks if the native binary
is available, otherwise it tries to run the jar.

The native binary is considerably faster than the jar, but it can cause issues if it needs
to deserialize a system model (via `--load` or `--diff`) or system model template containing
unknown types (e.g. from `withValue()`).


## Caveats and limitations
- no flow analysis making precise entity identification difficult for e.g. dispatcher-like 
  methods dealing with multiple entity types.
