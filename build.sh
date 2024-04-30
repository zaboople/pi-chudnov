#!/bin/bash
cd $(dirname $0)
rm -rf build
mkdir -p build
find java -name '*.java' | xargs javac -Xlint:deprecation -d build || exit 1
cp java/org/tmotte/chudnov/*.txt build/org/tmotte/chudnov/

