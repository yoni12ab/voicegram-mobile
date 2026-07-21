#!/bin/bash
# Script to build VoiceGram with Java 17
export JAVA_HOME=/Users/yoni/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew "$@"