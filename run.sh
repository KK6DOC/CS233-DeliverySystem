#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 17 -d build/classes src/*.java
exec java -cp build/classes Main "$@"
