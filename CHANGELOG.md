## Upcoming release

### New/Tweaks
- dot: Remove need for specifying `"dot-ignore"` property.
- CLI: `--edge-layout=spline|polyline|ortho`, used together with `-R`/`--render`.
- `spring-axon`: member aggregate label changed to `$aggregate[$member]`.

### Breaking changes
- `DSL.Instrumenter.graphviz()`: parameter `stripLabelSuffix` renamed to `removeSuffix`.
- Entity Types can can only be associated with a single element type (class, method, etc). 
- Internal visibility on many classes previously marked as public.

### Fixes
- `spring-axon`: fix member aggregate identification when field type is a Map. 

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
