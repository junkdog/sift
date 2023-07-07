## Upcoming Release

## sift-0.14.0 2023-07-07

### Improved Element Tracking Performance
The tracking of element associations during template execution in Sift has undergone substantial performance
enhancements. Traced elements are now assigned unique integer identifiers, facilitating the replacement of 
the previously tracked `List<Element>` with an `int[]`. This greatly improves cache locality, which in
turn, leads to much faster comparisons.

### Fixes
- StackOverflowError when resolving formal type parameters of generic enum.
- DSL: `readType()` of signatures could omit the package name when constructing the type.
- Exception during `--statistics` on empty results.


## sift-0.13.0 2023-07-02

### Annotations scope
The annotations scope enables working with annotations beyond reading top-level elements. It provides functions
to traverse nested annotations and to initiate a class scope from types referenced by annotations.

`annotations(filter: SiftType?) {}` has been added to all scopes representing elements which can be annotated.
The filter parameter is optional and defaults to `null`, hence, if not specified, all annotations get returned.

Three unique functions are included within the annotations scope:
- `nested(element) {}`: Iterates child annotations referenced by the specified element. The `element` parameter
  corresponds to the name of the annotation element which nested annotations are to be accessed.
- `explodeTypes(element, synthesize) {}`: Iterates over classes stored within a specific annotation element. The
  parameter `element` represents the name of the annotation element to be processed. `synthesize` is an optional
  parameter, defaulting to `false`. When set to `true`, missing types are stubbed based on those stored within the
  annotation element. `synthesize` is useful for stubbing classes not included with the input classes, providing a
  mechanism to register entities over unknown types.
- `readAttribute(attribute)`: operates similarly to `readAnnotation(type, attribute)`, but reads the attribute from
  the currently scoped annotations instead of the specified type.
  

### Method selection filter

The selection enum definition for `methods(<selection>) {}` has been updated to include the following options: 

- `constructors`: Matches all constructors in a class.
- `staticInitializers`: Matches the class initializers.
- `declared`: Matches all methods that are directly declared by the class, excluding constructors. This is the default filter.
- `inherited`: Matches all declared and inherited methods, excluding constructors.
- `synthetic`: Matches synthetic methods, such as default-value functions in Kotlin.
- `accessors`: Matches Kotlin's property getters and setters.

It is possible to compose multiple selection options in the `methods()` function by using the `+` operator, like so:
`methods(declared + constructors) {}`.

Note that constructors are no longer included by default.

### New/Tweaks
- New property text transformers: `uppercase()` and `lowercase()`.
- DSL: added `label` overloads for `classes()`, `fields()`, `methods()`, `parameters()`, `classesOf()`, `fieldsOf()`,
  `methodsOf()`, `parametersOf()`, `elementsOf()` and `outerScope()`. The label is displayed when running with
  `--profile`, under the associated `fork` action.

### Breaking changes
- DSL: entity properties from `readAnnotation()` are now flattened if they are arrays.

### Fixes
- DSL: `property()` ignored `PropertyStrategy` when used together with `entity()` registration.
- DSL: `readAnnotation()` now converts `org.objectweb.asm.Type` to `sift.core.dsl.Type` for fields containing class values.
- DSL: `readAnnotation()` the `field` parameter is now more appropriately named 'attribute'.

## sift-0.12.0 2023-06-17

### Kotlin language support

This update incorporates Kotlin language support:
- Extension function names are rendered appropriately.
- Value classes, formerly known as inline classes, now display the value class name instead of the underlying type.
- The enumeration of methods and parameters has been improved for relevance.
- It's now possible to filter based on Kotlin's internal visibility modifier.
- Visibility of properties now mirrors the associated getter method's visibility, instead of the backing field's private visibility.

Kotlin support is achieved through parsing `@kotlin.Metadata` class annotations, utilizing
the [kotlin-reflect-lite][kotlin-reflect-lite] library. 

#### Selection enum for `methods()` and `parameters()`
DSL functions `parameters() {}` and `properties() {}` now take an optional `ParameterSelection` and
`MethodSelection` enum, in order to make it easier to iterate over the pertinent elements.

The `ParameterSelection` enum can be used with the `parameters()` function. It provides the following options:
- `all`: Iterates over all parameters including Kotlin's extension receivers. This is the default behavior, matching previous functionality.
- `excludingReceiver`: Iterates over all parameters, but excludes the extension receiver in Kotlin.
- `onlyReceiver`: Focuses solely on the extension receiver, excluding all other parameters.

The `MethodSelection` can be used with `methods()`:
- `declared`: Selects all methods directly declared by the class, excluding Kotlin property accessors.
- `inherited`: Includes all declared methods and inherited methods.
- `declaredAndAccessors`: Similar to declared, but also includes Kotlin property accessors.

### `property()` extraction with `andThen(TextTransformer)`
Previously, the primary method for updating an entity label was to pass `TextTransformers` to `label()`. For instance:

```kotlin
annotatedBy(httpMethod)
entity(E.endpoint,
    label("\${http-method} /\${base-path:}/\${path:}",
        dedupe('/')), // dedupe is a TextTransformer
    ...
```

This approach served well for scenarios like substituting common paths/names or removing duplicate separators, but was
somewhat cumbersome for handling finer, more intricate transformations.

To address this, the `andThen` infix function has been introduced to enable direct transformation of entity properties:

```kotlin
classes {
    entity(e, label("CLS \${name}"),
        property("name",
            readName() andThen replace(Regex("^CommonPrefix"), ""))
    )
}
```

In the example, `property("name")`, `readName()` extracts the class name, `andThen` applies `replace(Regex("^CommonPrefix"), "")`, 
removing any starting "CommonPrefix" from the `name` property. 

### New property TextTransformer function: `stylize(TextStyle)`

The `stylize` function wraps a `TextStyle` around the text. For example, `stylize(Gruvbox.orange2 + bold)`. This
is also how `--template sift` renders the function parameter names with a different color compared to the function name:

```kotlin
parameters(excludingReceiver) {
    property(E.dsl, "params", readName() andThen stylize(blue2))
```

The current list of `TextTransformer` functions are:
- `edit`: takes a list of text transformers and applies them in sequence. 
- `dedupe`: removes duplicate instances of a specified character.
- `replace`: replaces all occurrences of a specified string or regular expression with a given replacement string.
- `idSequence`: replaces matches of a given regular expression with sequentially assigned values, starting at 1.
- `uuidSequence`: replaces UUID:s with sequentially assigned values, starting at 1.
- `stylize`: applies a `TextStyle`, for example`(Gruvbox.orange2 + bold)`, to the text.

### New/Tweaks
- DSL: `filter(Visibility)` added to class, field and method scopes. For kotlin properties, field visibility
  is reported as the visibility of the property getter.
- DSL/Signature: `readType()` added to signature scope.
- Entity label parameters now always return all values. Previously, only the initial value was utilized unless
  the property name was prefixed with +.


### Breaking changes
- DSL: `editText()` has been renamed to `editor()`. This function is currently only used during graphviz dot generation,
  and will likely be removed in later releases.

### Fixes
- SiftType `Type::simpleName` now correctly handles types with generic signatures.
- DSL/Parameter: fix `readType()` serialization key colliding with `readType()` in class scope. 
- DSL/Signature: `filter()` has been enhanced to check against both the full and simple name signatures for each type,
  fixing the previous limitation of considering only the raw type.
- `PropertyStrategy.unique` now takes the set of its own input to ensure distinct values.
- DSL/Signature: `explodeType()` and `explodeTypeT()` no longer output duplicate types.

 [kotlin-reflect-lite]: https://github.com/Kotlin/kotlinx.reflect.lite


## sift-0.11.0 2023-06-06

### Error Handling
sift's error reporting mechanism has been improved and now supports integration with the `--profile` option.

![profile-trace-error](docs/images/sift-profile-trace-error.png)

The `--profile` option assists in identifying problematic DSL methods by marking the operation causing the
error. This feature enables traceability back to the template DSL.  

  
### New/Tweaks
- `--no-emoji` option to disable emoji output, for terminals that do not support it. The
  implementation is coarse, but should work for most emoji. 
- Added `jakarta-ws`, "Supporting template for Jakarta RESTful Web Services".

### Fixes
- DSL/Class: `implements()` now correctly matches types even when they are not incorporated within the input classes.
- SiftType `RegexType::simpleName` trims the regex pattern to only include the substring following the last dot. 

### Breaking changes
- DSL: The complete parameter sequence for the `property()` method has been revised to (entity, key, strategy, extract).
  For the relevant overloads that include `strategy`, it is now positioned prior to the `extract` parameter.


## sift-0.10.0 2023-05-17

![sift --template sift -f "net.onedaybeard.sift:core:0.10.0" --diff "net.onedaybeard.sift:core:0.9.0"][sift-dsl]

The `sift` template now enables direct comparison between releases without any
local prerequisites. Previously, one needed to have the older `.jar` locally available and execute a `--save` on
the older version before running `--diff <OLD_VERSION.JSON>`. The new release simplifies the process. Now,
the `--diff` command can be applied directly to Maven coordinates, eliminating the previous steps. For example:
`sift --template sift -f "net.onedaybeard.sift:core:0.10.0" --diff "net.onedaybeard.sift:core:0.9.0"` to
identify changes to the API in the latest release.

 [sift-dsl]: https://raw.githubusercontent.com/junkdog/sift/main/docs/images/sift-diff-maven-coords.png
 [sift-dsl-t]: docs/images/sift-diff-maven-coords_thumbnail.png

### New/Tweaks
- Introduced `RegexType` for flexible type matching using regular expressions. Use the `.regexType`
  extension function on a string, or `.type` on a Regex, to create a RegexType. For example,
  `Regex("java.util.*").type` matches all types in the java.util package. DSL methods `implements()`,
  `filterType()` and `annotatedBy()` have been updated accordingly.  
- `-f` option has been updated to also support URI paths and maven coordinates, for example: 
  `sift -f https:///path/to/classes.jar ...` and `sift -f net.onedaybead.sift:core:0.9.0 ...`.
- `--diff` now also works against class directories, jars, URI:s and maven coordinates. This
  means that it is no longer necessary to `--save` the system model before running `--diff`, e.g.:
  `sift --template sift -f net.onedaybead.sift:core:0.9.0 --diff net.onedaybead.sift:core:0.7.0`
- `--maven-repository` option has been added to specify additional Maven repositories for downloading artifacts.
  By default, Maven Central and local user repositories are always included.
- Remaining usages of `org.objectweb.asm.Type` replaced with `sift.core.dsl.Type`.
- `sift` template: DSL methods for dealing with entity properties are now prefixed with a :gear: icon.
- DSL: The `fork` actions displayed when using the `--profile` flag now incorporate the
  labels from the `scope()` functions.  

### Breaking change
- `siftrc.sh` and `siftrc.zsh` replaced by `sift.config`. Update custom configurations to this new file.


## sift-0.9.0 2023-04-22

### Entity Registration with Generic Types

Entities can now be registered against generic type signatures, such as `Repository<User>`. This first iteration
does not cover all use-cases, specifically type arguments inferred from context are currently not resolved,
leading to sift ignoring the generic signatures of method invocations.

### Breaking change: `sift.core.dsl.Type` supersedes `org.objectweb.asm.Type` 

The `sift.core.dsl.Type` class supersedes `org.objectweb.asm.Type` usage in the DSL. The new type
class supports incorporating generic type information and is generally easier to work with. Instances
of the new `Type` class are created with the `type()` function or the `String.type` extension property,
for example: `type("java.lang.String")` and `"java.lang.String".type`.

### Breaking change: entity property update strategies
Entity properties now overwrite existing properties by default, instead of appending to them . You can
can modify this behavior by specifying the `strategy` parameter in the `property()` function. For example:

```kotlin
property(PropertyStrategy.unique, foo, "labels", withValue("hello"))
```

Property strategies are one of: `replace`, `append`, `prepend`, `immutable` and `unique`. 

### New/Tweaks
- DSL/Class: added `inherited` parameter to `methods()` and `fields()` to include inherited methods and fields.
- Enhanced performance primarily focused on element trace optimizations - the process of interrelating
  elements as they are navigated by the DSL. This enhancement reduces the total execution time by up to
  40% for complex templates.
- DSL/Method: `filterName()` added string overload for `name`.
- DSL/Method: `fieldAccess {}` to iterate accessed fields.
- Entity elements of `Entity.Type.fieldAccess` can now be either fields or classes. If the latter, 
  then the type of the field is used.
- siftrc.zsh|sh is now created under `~/.local/share/sift/`. It currently holds `SIFT_ARGS`, CLI options
  always appended.
- `--stastistics` prints statistics about the state from the execution of the system model template.


### Breaking changes
- TODO: methods affected by `Type` change
  
### Fixes
- sift template: `object` Actions were not associated with their DSL methods.

## sift-0.8.0 2023-03-20

### Example template: Game Rental Sample Application

A short [template][example-gamerental] for the Game Rental demo application. It demonstrates
how to work with JPA entities, and wires axon entities against `@MessageMapping` endpoints.

 [example-gamerental]: template-examples/spring-axon-gamerental 

### New/Tweaks
- `--tree-root` option is now repeatable, allowing you to specify multiple roots.
  For example: `sift -t spring-axon -r command -r query ...`
- `--list-templates` option now displays a brief description for each template.
- `--dump-system-model` option now converts entity UUIDs to integers, making the output more readable.

### Fixes
- `DSL.property()`: would sometimes fail to update entity properties.
- `sift` template: fix DSL registration.
- IllegalArgumentException for methods declaring thrown exceptions as generic types, e.g.:
```java
public static <X extends Throwable> void propagate(Throwable t) throws X
```
- `spring-axon`: no longer fails if a `@Controller` doesn't register any REST endpoints.


## sift-0.7.0 2023-03-06

DSL documentation published to https://junkdog.github.io/sift/ 

### Example template: Spring PetClinic Sample Application

This is a short, [custom template][example-template] for the Spring Boot Pet Clinic demo
application. It demonstrates how to combine existing templates with custom templates,
describes relationships between entities, and configures properties for Graphviz visualization.

![petclinic](docs/images/sift-example-template-petclinic.png)

 [example-template]: template-examples/spring-petclinic-template

### New/Tweaks
- `--profile` now includes `ety#` column, tracking entity registrations.
- `--stacktrace` to print stacktrace on errors.
- DSL/Core: `editText(TextTransformer...)` for`property()`, currently used by `graphviz()`.
- DSL/Elements: inherits from `Core`, making it a bit more useful.

### Breaking changes
- DSL: changed package to `sift.core.dsl` from `sift.core.api`.
- DSL/Template: `graphviz(label=List<TextTransformer>)` replaces `removeSuffix` argument.   
- All scopes (Template, Classes, Methods...) are now top-level classes.
- DSL/Core: restricted value of `withValue(value)` to String, Number, Boolean, Enum. This 
  solves a lot of issues with serialization.

### Fixes
- DSL/property: Improved the `property()` method to resolve relationships via direct association 
  first. If direct association fails for all entities, it falls back on identifying relationships
  through shared common associations. The old behavior was to rely solely on shared common
  associations, but this could produce false positives. 
- Fix NPE when `--list-entity-types` is used with no path argument.
- `--list-entity-types` now resolves entity counts when system model is deserialized with `--load`.

## sift-0.6.0 2023-02-26

### New/Tweaks
- DSL.Template: `classesOf()`/`methodsOf()`/`fieldsOf()` now passes along the Entity.Type as a lambda parameter. 
- DSL.Template: `entity[key] = foo.(instantations|invocation|fieldAccess)`, replacing `registerInvocationsOf` and `registerInstantiationsOf`. 
- DSL.Class: `interfaces(recurisve, synthesize)` iterates interfaces of inspected class nodes. 
- DSL.Class: `filter(Modifiers..., invert)` iterates classes with matching modifiers. 
- DSL.Class: `enums {}` opens a field scope iterating all enum values (as static final fields). 
- DSL.Field: `filterType()` of fields. 
- DSL.Field: `filter(Modifiers..., invert)` iterates fields with matching modifiers. 
- DSL.Method: `filter(Modifiers..., invert)` iterates methods with matching modifiers. 
- DSL.Parameter: `filterType()` of parameters. 
- dot: Remove need for specifying `"dot-ignore"` property.
- DSL: re-registering the same entity using `entity(entitty, label...)` will now update the entity label.
- CLI: `--edge-layout=spline|polyline|ortho`, used together with `-R`/`--render`.
- `spring-axon`: member aggregate label changed to `$aggregate[$member]`.
- `spring-boot`: base template, extended by `spring-axon`.

### Breaking changes
- Instrumenter Service renamed to System Model Template:
  - pipeline() is now template()
  - `instrumenter {}` is now `template {}`
- `DSL.Instrumenter.graphviz()`: parameter `stripLabelSuffix` renamed to `removeSuffix`.
- Entity Types can can only be associated with a single element type (class, method, etc). 
- Internal visibility on many classes previously marked as public.

### Fixes
- `spring-axon`: fix member aggregate identification when field type is a Map. 
- `spring-axon`: improved identification of projections.
- Inadvertent exclusion of entities from the tree if the added entity also occurred as a parent.
- graphviz: childless entities could throw a NoSuchElementException.


## sift-0.5.0 2023-01-14

### Reverse registrations 

"Reverse registration" of invocations and instantiations of entities is now possible. This means that,
in addition to `foo["a"] = bar.instantations` and `foo["b"] = bar.invocations`, we can now also register
instances of `foo` that are invoked or instantiated by `bar`:  

```kotlin
methods {
    // note that `bar` represents the contextual method elements in the current scope
    foo.invocations["invoked-by"] = bar
    payload.instantations["created-by"] = bar
}
```

### Customizing entity labels with TextTransformers

The TextTransformer interface provides static functions for updating entity labels:

- `dedupe`: removes duplicate instances of a specified character.
- `replace`: replaces all occurrences of a specified string or regular expression with a given replacement string.
- `idSequence`: replaces matches of a given regular expression with sequentially assigned values, starting at 1.
- `uuidSequence`: replaces UUID:s with sequentially assigned values, starting at 1.

The `label` function has been updated to take TextTransformers as a vararg parameter, and applies them in order:

```kotlin
entity(E.endpoint,
    label("$method /\${base-path:}\${path:}",
        dedupe('/'),                 // drop repeating '/'
        replace(COMMON_PREFIX, "")), // shorten URL
    property("path", readAnnotation(httpMethod, "value"))
)
```

### Elements: Scope-erased scope

The `Elements` context is a type-erased scope that only provides methods defined by the `Core` scope class.
This new scope adds a layer of abstraction to the DSL, making it easier to reuse code and update properties
in a more flexible way.

### New
- DSL.Parameter: generic `signature {}` scope.
- DSL.Method: Reverse association with `foo.instantiations["created-by] = bar` and `foo.invocations["called-by] = bar` 
- DSL.Signature: `explodeTypeT(signature) {}` extracts type `T` from signature pattern, e.g. `Map<String, List<T>>`. `_` matches any type. 
- DSL.Instrumenter: `elementsOf(e) {}` iterate over all entities without element-specific methods. Useful for property tagging. 
- DSL.Instrumenter: `graphviz(e, ...)` which allows setting graphviz properties such as identifier, rank, type, shape, style, etc.
- DSL: `filter(string)` is a short-hand for `filter(Regex.fromLiteral(string))`
- `spring-crud` instrumenter - very similar to `jpa`.

### Breaking
- Multiple classes changed packages and visibility; ongoing.
- `Gruxbox` and `Style` moved to `sift.core.terminal` from `sift.instrumenter`.
- SPI: `InstrumenterServiceProvider` is now optional when implementing `InstrumenterService` since json
  serialization is typically more convenient. 
- Due to some internal renaming, any custom pipelines need to be recompiled/resaved to work with the new version.
- `Style.plain()` no longer accepts optional `dedupe` argument as that functionality is now taken care of by TextTransformers.

### Fixes
- Entities were sometimes not relatable when calling `property()` from a scope not directly related to the scope
  which registered the entity. 

  
## sift-0.4.0 2022-12-07

### New
**Signature Scope**: Limited DSL support for exploring generics/type signatures, e.g. `Foo` inside `List<Foo>`.
Note that entities cannot be registered inside the signature scope. The type argument has to be registered by
its raw class, using `explodeType() {}`.

Support for method parameter signatures is currently lacking from the DSL.

**`sift` Self Instrumenter**: A new instrumenter has been added that extracts the system model centered around
the DSL. This instrumenter allows you to compare the changes to the DSL between releases using the `--diff` option.
Currently, Sift does not have support for resolving Kotlin constructs, so some of the DSL method names may
be mangled.

![sift diff 0.4.0 vs 0.3.0](docs/images/sift-diff-0.4.0-0.3.0.png)

- DSL.Class: `outerScope {}` inner classes iterate over their outer classes.
- DSL.Class: `superclassSignature(synthesize=false) {}` - parent class signature `Foo<Bar>`; skips non-generic parents (WIP).
- DSL.Field: `explodeType(synthesize=false) {}` iterates class elements of fields.
- DSL.Field: `signature(synthesize=false) {}` - generic field signature; skips non-generic fields.
- DSL.Method: `filterName()` only inspects the method name; `filter()` also checks the class name.
- DSL.Method, DSL.Field, DSL.Parameter: entity property method `readName()`.
- DSL.Method: `returns(synthesize=false) {}` signature scope for method return 
- DSL.Signature: `explodeType(synthesize=false) {}` - class scope of current type arguments.  
- DSL.Signature: `filter(Regex)` - filters concrete type argument by class name.
- DSL.Signature: `scope {}` - local scope, as found elsewhere.
- DSL.Signature: `typeArgument(index) {}` - filters nested type argument by position.
- DSL.Signature: `typeArguments {}` - iterate nested type arguments.
- DSL: `readName(shorten=true)` shortens names of inner classes.
- Dot entity property `dot-shape` accepts valid graphviz shapes
- Entity labels: extract all
- property values by prefixing the property name with  `+`. E.g. `${+property}`.

### Fixes
- DOT: illegal node ids containing `.` and `$`.  

### Changes
- graphviz property `dot-id` renamed to`dot-id-as`. 
- updated spring-axon grahpviz to use `dot-shape`.


## sift-0.3.0 2022-11-18

### Breaking
- DSL: `update` used for updating properties on existing entities is renamed to `property`. 
- DSL: `parentScope` renamed to `outerScope` in the name of clarity.  

### Fixes
- introspection of kotlin's noinline lambdas  

### New
- CLI: `--render`/`-R` prints entities in graphviz's DOT language. `sift.zsh` and `sift.sh` automatically
  pipe to graphviz and displays the diagram inline. Note that `-R` will print the DOT script to stdout.
![sift-render](docs/images/sift-spring-axon-render.png)
- CLI: `--dump-system-model` prints all entities and their properties. Primarily useful for debugging.  
- `sift.sh`, QoL script similar to `sift.zsh`

### Changes
- `mvn install` installs the executable into `~/.local/share/sift/bin`. If compiling with `-P native-image`,
  the native sift executable is also copied. 
- "backtrack" references are now added to all child entities. 


## sift-0.2.0 2022-11-11
### Breaking
- CLI: `-f` replaces PATHS argument; filter changed to `-F`

### Features
- CLI: `--version` option
- CLI: `--save`/`-s` save the resulting system model as json
- CLI: `--diff` compares a previous model with the current state 
- CLI: `--load` restores a previously saved model instead of instrumenting classes 


## sift-0.1.0 2022-11-06
- initial release
