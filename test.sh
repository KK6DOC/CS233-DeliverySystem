#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/test-classes
javac --release 17 -Xlint:all -d build/test-classes src/*.java tests/*.java
java -ea -cp build/test-classes ProjectTest
java -ea -cp build/test-classes TerminalFlowTest
if [ "${1:-}" = "--desktop" ]; then
    java -Dapple.awt.UIElement=true -ea -cp build/test-classes DesktopFlowTest
    java -Dapple.awt.UIElement=true -cp build/test-classes UiPreview
fi
