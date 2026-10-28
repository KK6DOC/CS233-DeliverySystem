#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/app
javac --release 17 -d build/classes src/*.java
jar --create --file build/app/CS233DeliverySystem.jar --main-class Main -C build/classes .
cp Menu.txt build/app/Menu.txt
echo 'Built build/app/CS233DeliverySystem.jar'
