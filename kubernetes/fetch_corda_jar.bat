@echo off
curl -o party-a/corda.jar https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases/net/corda/corda/4.0/corda-4.0.jar
copy party-a\corda.jar party-b\corda.jar 
copy party-a\corda.jar party-c\corda.jar 
