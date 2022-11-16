#!/usr/bin/env zsh

# prereq: mvn clean install, alt mvn clean install -P native-image (graalvm)
VERSION="0.3.0-SNAPSHOT"
SIFT_M2_PATH="$HOME/.m2/repository/net/onedaybeard/sift/cli/$VERSION"
SIFT_JAR="$SIFT_M2_PATH/cli-$VERSION.jar"
SIFT_BIN="$SIFT_M2_PATH/sift"

# common args
SIFT_ARGS=(
    --ansi=truecolor
)

# base graphviz dot args
DOT_ARGS=(-Tpng -Gmargin=0)

# error codes
ERROR_SIFT_NOT_FOUND=2
ERROR_GRAPHVIZ_NOT_FOUND=3
ERROR_IMAGE_VIEWERS_NOT_FOUND=4

# set SIFT_DEBUG to any value to collect profiling data for use with
# kcachegrind: building with the 'native-image-debug' maven profile is
# a prerequisite for source code-level analysis
if (( ${+SIFT_DEBUG} )) ; then
    SIFT_VALGRIND=(
        valgrind
            --tool=callgrind
            --dump-instr=yes
            --simulate-cache=yes
            --collect-jumps=yes
    )
fi

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
    [[ ${*[(ie)--render]} -le ${#*} ]]
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
        SHOW_IMG=(kitty +kitten icat --align left)
    elif exists feh ; then
        SHOW_IMG=(feh --auto-zoom --scale-down -)
        DOT_ARGS+=(-Gbgcolor=black)
    elif exists display ; then
        SHOW_IMG=(display)
        DOT_ARGS+=(-Gbgcolor=black)
    else
        echo "Error: Unable to find image viewers 'feh' or 'display'" >&2
        exit $ERROR_IMAGE_VIEWERS_NOT_FOUND
    fi

    _sift $* | dot $DOT_ARGS | ${=SHOW_IMG}
else
    _sift $* | less -FXRS
fi
