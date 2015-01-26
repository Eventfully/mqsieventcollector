@echo off
setlocal
cd ..
call environment.bat
%JAVA_HOME%\bin\java -cp "scripts\lib\*;lib\*;" groovy.lang.GroovyShell scripts\mqsibrokerutil.groovy %*
cd bin
endlocal