#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
JAVAC="${JAVAC:-/opt/homebrew/opt/openjdk@17/bin/javac}"
JAVA="${JAVA:-/opt/homebrew/opt/openjdk@17/bin/java}"
mkdir -p build/tests
"$JAVAC" --release 8 -d build/tests src/org/opendash/diagnostics/BoundedCommand.java tests/BoundedCommandTest.java
"$JAVA" -cp build/tests org.opendash.diagnostics.BoundedCommandTest
