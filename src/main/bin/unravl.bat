@echo off

Rem Run UnRAVL's main entry point.
Rem Command line arguments are UnRAVL script files to execute:
Rem unravl.bat script1.json script2.json script3.json

set UNRAVL_DIR=%~dp0..\..\..
set UNRAVL_JAR_DIR=%UNRAVL_DIR%\build\libs
set UNRAVL_LIB_DIR=%UNRAVL_DIR%\build\output\lib

java -Dapp.name=UnRAVL ^
        -classpath %UNRAVL_JAR_DIR%\*;%UNRAVL_LIB_DIR%\* ^
        %UNRAVL_OPT% ^
        com.sas.unravl.Main ^
        "%*"
