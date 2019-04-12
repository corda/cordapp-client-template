#!/bin/sh

# If variable not present use default values
: ${CORDA_HOME:=/opt/corda}
: ${JAVA_OPTIONS:=-Djava.security.egd=file:/dev/urandom -Dlog4j.configurationFile=./log4j2.xml}

export CORDA_HOME JAVA_OPTIONS

cd ${CORDA_HOME}

java ${JAVA_OPTIONS} -jar ${CORDA_HOME}/corda.jar --just-generate-node-info --dev-mode && \
java ${JAVA_OPTIONS} -jar ${CORDA_HOME}/corda.jar
