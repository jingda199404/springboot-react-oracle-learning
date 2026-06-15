#!/bin/sh

PROJECT_ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

export JAVA_HOME="$PROJECT_ROOT/.tools/jdk-21/Contents/Home"
export MAVEN_HOME="$PROJECT_ROOT/.tools/maven"
export NODE_HOME="$PROJECT_ROOT/.tools/node"
export COLIMA_HOME="$PROJECT_ROOT/.tools/state/colima"
export LIMA_HOME="$PROJECT_ROOT/.tools/state/lima"
export DOCKER_CONFIG="$PROJECT_ROOT/.tools/docker-config"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$NODE_HOME/bin:$PROJECT_ROOT/.tools/docker/bin:$PROJECT_ROOT/.tools/colima/bin:$PROJECT_ROOT/.tools/lima/bin:$PATH"

echo "Project development environment loaded."
echo "JAVA_HOME=$JAVA_HOME"
