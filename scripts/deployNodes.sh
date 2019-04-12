#!/usr/bin/env bash

cd ../

killall java -9

rm -rf kotlin-source/build/nodes

./gradlew clean
./gradlew deployNodes
./gradlew --stop

