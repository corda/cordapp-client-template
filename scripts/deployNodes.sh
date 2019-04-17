#!/usr/bin/env bash

cd ../

killall java -9

rm -rf build/nodes

./gradlew clean
./gradlew deployNodes
./gradlew --stop

cp membership-service.conf build/nodes/Notary/cordapps/config/.

cp membership-service.conf build/nodes/Worldpay_BNO/cordapps/config/.

cp membership-service.conf build/nodes/PartyA/cordapps/config/.

cp membership-service.conf build/nodes/PartyB/cordapps/config/.

cp membership-service.conf build/nodes/PartyC/cordapps/config/.


