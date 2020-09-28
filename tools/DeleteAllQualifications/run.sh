#!/bin/sh

mvn clean
mvn compile
mvn -X exec:java -Dexec.args="$1 $2"
