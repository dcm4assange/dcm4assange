@echo off
rem -------------------------------------------------------------------------
rem storescp  Launcher
rem -------------------------------------------------------------------------

if not "%ECHO%" == ""  echo %ECHO%
if "%OS%" == "Windows_NT"  setlocal

set MAIN_MODULE=org.dcm4assange.tool.storescp
set MAIN_JAR=dcm4assange-tool-storescp-${project.version}.jar

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

rem Read all command line arguments

set ARGS=
:loop
if [%1] == [] goto end
        set ARGS=%ARGS% %1
        shift
        goto loop
:end

if not "%DCM4CHE_HOME%" == "" goto HAVE_DCM4CHE_HOME

set DCM4CHE_HOME=%DIRNAME%..

:HAVE_DCM4CHE_HOME

if not "%JAVA_HOME%" == "" goto HAVE_JAVA_HOME

set JAVA=java

goto SKIP_SET_JAVA_HOME

:HAVE_JAVA_HOME

set JAVA=%JAVA_HOME%\bin\java

:SKIP_SET_JAVA_HOME

set CP=%DCM4CHE_HOME%\etc\storescp

set MP=%MP%;%DCM4CHE_HOME%\lib\%MAIN_JAR%
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-conf-model-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-core-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-net-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-elmdict-acuson-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-elmdict-agfa-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-elmdict-camtron-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-elmdict-elscint-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-elmdict-gems-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\dcm4assange-elmdict-hitachi-${project.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\slf4j-api-${slf4j.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\logback-core-${logback.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\logback-classic-${logback.version}.jar
set MP=%MP%;%DCM4CHE_HOME%\lib\picocli-${picocli.version}.jar

"%JAVA%" %JAVA_OPTS% -cp "%CP%" -p "%MP%" -m %MAIN_MODULE% %ARGS%
