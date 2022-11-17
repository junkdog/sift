## Upcoming release
### Breaking
- DSL: `update` used for updating properties on existing entities is renamed to `property`. 

### Features
- CLI: `--render`/`-R` prints entities in graphviz's DOT language. `sift.zsh` and `sift.sh` automatically
  pipe to graphviz and displays the diagram inline. Note that `-R` will print the DOT script to stdout.
- CLI: `--dump-system-model` prints all entities and their properties. Primarily useful for debugging.  
- `mvn install` installs the executable into `~/.local/share/sift/bin`. 
- DSL: boolean parameter `ignoreOthers` on `classesOf` ignores non-class elements when iterating
  the input elements. This is primarily useful for utility functions e.g. updating properties
  on multiple entity types.
- `sift.sh`, QoL script similar to `sift.zsh`

### Changes
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
