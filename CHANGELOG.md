## Upcoming release


## sift-0.3.0 2022-12-07

### New
**Signature Scope**: Limited DSL support for exploring generics/type signatures, e.g. `Foo` inside `List<Foo>`.
Note that entities cannot be registered inside the signature scope. The type argument has to be registered by
its raw class, using `explodeType() {}`.

Support for method parameter signatures is currently lacking from the DSL.

**`sift` Self Instrumenter**: A new instrumenter has been added that extracts the system model centered around
the DSL. This instrumenter allows you to compare the changes to the DSL between releases using the --diff option.
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
- Entity labels: extract all property values by prefixing the property name with  `+`. E.g. `${+property}`.

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
