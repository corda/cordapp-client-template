#!/usr/bin/env bash

cd ../

killall java -9

rm -rf build/nodes

./gradlew clean
./gradlew deployNodes
./gradlew --stop

cp conf/membership-service.conf build/nodes/Notary/cordapps/config/.
cp conf/billing-app.conf build/nodes/Notary/cordapps/config/.

cp conf/membership-service.conf build/nodes/Worldpay_BNO/cordapps/config/.
cp conf/billing-app.conf build/nodes/Worldpay_BNO/cordapps/config/.

cp conf/membership-service.conf build/nodes/PartyA/cordapps/config/.
cp conf/billing-app.conf build/nodes/PartyA/cordapps/config/.

cp conf/membership-service.conf build/nodes/PartyB/cordapps/config/.
cp conf/billing-app.conf build/nodes/PartyB/cordapps/config/.

cp conf/membership-service.conf build/nodes/PartyC/cordapps/config/.
cp conf/billing-app.conf build/nodes/PartyC/cordapps/config/.


