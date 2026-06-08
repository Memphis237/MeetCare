#!/bin/sh

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$APP_HOME/.mvn/wrapper/maven-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
  exec java ${JAVA_OPTS:-} ${MAVEN_OPTS:-} -classpath "$WRAPPER_JAR" -Dmaven.multiModuleProjectDirectory="$APP_HOME" org.apache.maven.wrapper.MavenWrapperMain "$@"
fi

exec mvn "$@"
