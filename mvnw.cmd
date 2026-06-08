@echo off
setlocal

set "APP_HOME=%~dp0"
set "WRAPPER_JAR=%APP_HOME%\.mvn\wrapper\maven-wrapper.jar"

if exist "%WRAPPER_JAR%" (
  java %JAVA_OPTS% %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%APP_HOME%" org.apache.maven.wrapper.MavenWrapperMain %*
  exit /b %ERRORLEVEL%
)

mvn %*
