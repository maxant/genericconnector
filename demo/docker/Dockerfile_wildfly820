#build using:
#  docker build -f Dockerfile_wildfly820 -t maxant/fedora23_jdk18_wildfly820 .

FROM maxant/fedora23_jdk18:latest
MAINTAINER Ant Kutschera <ant.kutschera@gmail.com>

WORKDIR /opt/
ADD wildfly-8.2.0.Final.tar.gz .

EXPOSE 8080

WORKDIR ./wildfly-8.2.0.Final/bin/

CMD ./standalone.sh -b 0.0.0.0


