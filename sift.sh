#!/usr/bin/env bash

# prereq: mvn clean install, alt mvn clean install -P native-image (graalvm)
SIFT_PATH="$HOME/.local/share/sift"
SIFT_JAR="$SIFT_PATH/bin/sift-cli.jar"
SIFT_BIN="$SIFT_PATH/bin/sift"
SIFT_RC="$SIFT_PATH/siftrc.sh"

# Check if siftrc.zsh exists, create it if not
if [ ! -f "$SIFT_RC" ]; then
    echo 'SIFT_ARGS="--ansi=ansi256"' > "$SIFT_RC"
fi

source "$SIFT_RC"

# common args

# error codes
ERROR_SIFT_NOT_FOUND=2
ERROR_GRAPHVIZ_NOT_FOUND=3
ERROR_IMAGE_VIEWERS_NOT_FOUND=4

function _sift() {
    if [[ -x $SIFT_BIN ]]; then
        $SIFT_VALGRIND $SIFT_BIN -Xmx256m -Xmn32m $SIFT_ARGS $*
    elif [[ -f $SIFT_JAR ]]; then
        java -jar $SIFT_JAR $SIFT_ARGS $*
    else
        echo "Error: Unable to find sift binary or jar" >&2
        exit $ERROR_SIFT_NOT_FOUND
    fi
}

function pipe_to_dot() {
    # checks for --render in args
    [[ "$@" =~ "--render" ]]
}

function exists() {
    command -v $1 &> /dev/null
}

if pipe_to_dot $* ; then

    if ! exists dot ; then
        echo "Error: Unable to locate the graphviz 'dot' tool" >&2
        exit $ERROR_GRAPHVIZ_NOT_FOUND
    fi

    if [[ $TERM == "xterm-kitty" ]]; then
        _sift $* | dot -Tpng -Gmargin=0 | kitty +kitten icat --align left
    elif exists feh ; then
        _sift $* | dot -Tpng -Gmargin=0 -Gbgcolor=black | feh --auto-zoom --scale-down -
    elif exists display ; then
        _sift $* | dot -Tpng -Gmargin=0 -Gbgcolor=black | display
    else
        echo "Error: Unable to find image viewers 'feh' or 'display'" >&2
        exit $ERROR_IMAGE_VIEWERS_NOT_FOUND
    fi

else
    _sift $* | less -FXRS
fi
