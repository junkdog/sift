#!/usr/bin/env bash

function _build {
    javac $2 ConstructA.java ConstructB.java Fn.java

    local destination=../core/src/test/resources/testdata/$1
    mkdir -p $destination
    mv *.class $destination/
}

cd $(dirname "$0")
_build no-debug "-g:none" && \
  _build vars "-g:vars" && \
  _build params "-parameters"
