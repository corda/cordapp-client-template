#!/bin/sh
curl -o party-a/corda.jar https://software.r3.com/artifactory/corda-releases/net/corda/corda/4.0/corda-4.0.jar
cp party-a/corda.jar party-b/corda.jar 
cp party-a/corda.jar party-c/corda.jar 
