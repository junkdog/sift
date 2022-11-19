## Upcoming release

### Features
- DSL: `readName()` now also works for methods, fields and parameters.
 

### Changes
- graphviz property `dot-id` renamed to`dot-id-as`.  

## sift-0.3.0 2022-11-18
### Breaking
- DSL: `update` used for updating properties on existing entities is renamed to `property`. 
- DSL: `parentScope` renamed to `outerScope` in the name of clarity.  

### Fixes
- introspection of kotlin's noinline lambdas  

### Features
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
