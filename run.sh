#!/bin/bash
cd $(dirname $0)
./build.sh || exit 1
java -Xmx268m -classpath build org.tmotte.chudnov.Main "$@"
