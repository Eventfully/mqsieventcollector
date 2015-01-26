@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  mqsieventcollector startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and MQSIEVENTCOLLECTOR_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\mqsieventcollector-0.0.1-SNAPSHOT.jar;%APP_HOME%\lib\CL3Export.jar;%APP_HOME%\lib\CL3Nonexport.jar;%APP_HOME%\lib\com.ibm.mq.axis2.jar;%APP_HOME%\lib\com.ibm.mq.commonservices.jar;%APP_HOME%\lib\com.ibm.mq.defaultconfig.jar;%APP_HOME%\lib\com.ibm.mq.headers.jar;%APP_HOME%\lib\com.ibm.mq.jar;%APP_HOME%\lib\com.ibm.mq.jmqi.jar;%APP_HOME%\lib\com.ibm.mq.jms.Nojndi.jar;%APP_HOME%\lib\com.ibm.mq.pcf.jar;%APP_HOME%\lib\com.ibm.mq.postcard.jar;%APP_HOME%\lib\com.ibm.mq.soap.jar;%APP_HOME%\lib\com.ibm.mq.tools.ras.jar;%APP_HOME%\lib\com.ibm.mqjms.jar;%APP_HOME%\lib\connector.jar;%APP_HOME%\lib\dhbcore.jar;%APP_HOME%\lib\fscontext.jar;%APP_HOME%\lib\jms.jar;%APP_HOME%\lib\jndi.jar;%APP_HOME%\lib\jta.jar;%APP_HOME%\lib\ldap.jar;%APP_HOME%\lib\providerutil.jar;%APP_HOME%\lib\rmm.jar;%APP_HOME%\lib\groovy-all-2.3.8.jar;%APP_HOME%\lib\commons-cli-1.2.jar;%APP_HOME%\lib\spring-boot-starter-web-1.2.0.RELEASE.jar;%APP_HOME%\lib\jackson-annotations-2.4.4.jar;%APP_HOME%\lib\jackson-core-2.4.4.jar;%APP_HOME%\lib\jackson-databind-2.4.4.jar;%APP_HOME%\lib\spring-boot-starter-actuator-1.2.0.RELEASE.jar;%APP_HOME%\lib\spring-boot-starter-1.2.0.RELEASE.jar;%APP_HOME%\lib\spring-boot-starter-log4j-1.2.0.RELEASE.jar;%APP_HOME%\lib\camel-spring-boot-2.15-SNAPSHOT.jar;%APP_HOME%\lib\camel-groovy-2.15-SNAPSHOT.jar;%APP_HOME%\lib\camel-jms-2.15-SNAPSHOT.jar;%APP_HOME%\lib\jolokia-core-1.2.3.jar;%APP_HOME%\lib\spring-boot-starter-tomcat-1.2.0.RELEASE.jar;%APP_HOME%\lib\hibernate-validator-5.1.3.Final.jar;%APP_HOME%\lib\spring-core-4.1.3.RELEASE.jar;%APP_HOME%\lib\spring-web-4.1.3.RELEASE.jar;%APP_HOME%\lib\spring-webmvc-4.1.3.RELEASE.jar;%APP_HOME%\lib\spring-boot-actuator-1.2.0.RELEASE.jar;%APP_HOME%\lib\spring-boot-1.2.0.RELEASE.jar;%APP_HOME%\lib\spring-boot-autoconfigure-1.2.0.RELEASE.jar;%APP_HOME%\lib\snakeyaml-1.14.jar;%APP_HOME%\lib\jcl-over-slf4j-1.7.7.jar;%APP_HOME%\lib\jul-to-slf4j-1.7.7.jar;%APP_HOME%\lib\slf4j-log4j12-1.7.7.jar;%APP_HOME%\lib\log4j-1.2.17.jar;%APP_HOME%\lib\camel-spring-2.15-SNAPSHOT.jar;%APP_HOME%\lib\camel-core-2.15-SNAPSHOT.jar;%APP_HOME%\lib\spring-jms-4.1.3.RELEASE.jar;%APP_HOME%\lib\spring-context-4.1.3.RELEASE.jar;%APP_HOME%\lib\spring-tx-4.1.3.RELEASE.jar;%APP_HOME%\lib\spring-beans-4.1.3.RELEASE.jar;%APP_HOME%\lib\json-simple-1.1.1.jar;%APP_HOME%\lib\tomcat-embed-core-7.0.57.jar;%APP_HOME%\lib\tomcat-embed-el-7.0.57.jar;%APP_HOME%\lib\tomcat-embed-logging-juli-7.0.57.jar;%APP_HOME%\lib\tomcat-embed-websocket-7.0.57.jar;%APP_HOME%\lib\validation-api-1.1.0.Final.jar;%APP_HOME%\lib\jboss-logging-3.1.3.GA.jar;%APP_HOME%\lib\classmate-1.0.0.jar;%APP_HOME%\lib\spring-aop-4.1.3.RELEASE.jar;%APP_HOME%\lib\spring-expression-4.1.3.RELEASE.jar;%APP_HOME%\lib\slf4j-api-1.7.7.jar;%APP_HOME%\lib\jaxb-core-2.2.11.jar;%APP_HOME%\lib\spring-messaging-4.1.3.RELEASE.jar;%APP_HOME%\lib\aopalliance-1.0.jar

@rem Execute mqsieventcollector
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %MQSIEVENTCOLLECTOR_OPTS%  -classpath "%CLASSPATH%" org.eventfully.mqsi.event.collector.Application %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable MQSIEVENTCOLLECTOR_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%MQSIEVENTCOLLECTOR_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
