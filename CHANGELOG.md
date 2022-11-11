## Upcoming release
### Features
- CLI: `--graph`/`-g` renders system model in graphviz's DOT language. `sift.zsh` and `sift.sh` automatically
       pipe to graphviz and renders the output inline.
- `mvn install -P install-sift` installs the executable into `~/.local/share/sift/bin` along with a
  symlink in `~/.local/bin`. If sift is also built with `-P native-image`, the native executable
  is symlinked instead of the sift. 


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
