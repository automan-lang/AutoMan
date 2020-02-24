#!/bin/sh

mvn -X exec:java -Dexec.args="$1"
