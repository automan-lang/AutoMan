#!/bin/sh

mvn --quiet clean
mvn --quiet compile
mvn --quiet exec:java -Dexec.args="$1"
