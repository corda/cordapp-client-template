# Base image
FROM anapsix/alpine-java:8u172b11_jdk_unlimited

ENV CORDA_VERSION=${BUILDTIME_CORDA_VERSION}
ENV JAVA_OPTIONS=${BUILDTIME_JAVA_OPTIONS}
ENV CORDA_HOME=/opt/corda
ENV PATH=${CORDA_HOME}:${PATH} HOME=${CORDA_HOME}

WORKDIR ${CORDA_HOME}

# Set image labels
LABEL net.corda.version = ${CORDA_VERSION} \
      maintainer = "<devops@r3.com>" \
      vendor = "R3"

RUN apk upgrade --update && \
    apk add --update --no-cache bash iputils curl && \
    rm -rf /var/cache/apk/* && \
    # Add user to run the app && \
    addgroup corda && \
    adduser -G corda -D -s /bin/bash corda && \
    # Create /opt/corda directory && \
    mkdir -p /opt/corda/plugins && \
    mkdir -p /opt/corda/logs

# Copy corda files
ADD --chown=corda:corda corda.jar               /opt/corda/corda.jar
ADD --chown=corda:corda log4j2.xml              /opt/corda/log4j2.xml
ADD --chown=corda:corda node.conf               /opt/corda/node.conf
ADD --chown=corda:corda cordapps/               /opt/corda/cordapps
ADD --chown=corda:corda additional-node-infos/  /opt/corda/additional-node-infos
ADD --chown=corda:corda certificates/           /opt/corda/certificates
ADD --chown=corda:corda drivers/                /opt/corda/drivers
ADD --chown=corda:corda persistence*            /opt/corda/

COPY run-corda.sh /run-corda.sh

RUN chmod +x /run-corda.sh && \
    sync && \
    chown -R corda:corda /opt/corda

# Working directory for Corda
WORKDIR /opt/corda
ENV HOME=/opt/corda
USER corda

EXPOSE 2222
EXPOSE 10003

# Start it
CMD ["/run-corda.sh"]