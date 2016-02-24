@echo off

Rem Run UnRAVL's main entry point.
Rem Command line arguments are UnRAVL script files to execute:
Rem unravl.bat script1.json script2.json script3.json
Rem See https://github.com/sassoftware/unravl

set UNRAVL_DIR=%~dp0..
set UNRAVL_DEV_DIR=%~dp0..\..\..

if exist %UNRAVL_DIR%\lib (
  Rem This works for a distribution, where UNRAVL is deployed in %UNRAVL_DIR%
  Rem the script is in %UNRAVL_DIR%\bin,
  Rem and all the dependent jars are in  %UNRAVL_DIR\lib%
  set UNRAVL_CLASSPATH=%UNRAVL_DIR%\lib/*;
) else (
  if exist %UNRAVL_DIR%\..\..\build\libs (
    Rem This is for use in the build environment where the script
    Rem is in src/main/bin
    Rem Build with:
    Rem    .\gradlew clean build copyDeps
    Rem Gradle will put the UnRAVL jar in build/libs
    Rem and dependent jars in build/output/lib
    set UNRAVL_JAR_DIR=%UNRAVL_DEV_DIR%\build\libs
    set UNRAVL_LIB_DIR=%UNRAVL_DEV_DIR%\build\output\lib
    set UNRAVL_CLASSPATH=%UNRAVL_LIB_DIR%/*;%UNRAVL_JAR_DIR%/*;
  ) else (
    echo %UNRAVL_DIR% does not contain libraries in lib or build\libs
    echo If in development, run:
    echo   .\gradlew clean build copyDeps
    exit /B 1
  )
)

if [%JAVA_HOME%] == [] (
   set java=java
) else (
   set java=%JAVA_HOME%\bin\java.exe
)

%java% -Dapp.name=UNRAVL ^
       -classpath "%UNRAVL_CLASSPATH%" ^
       %UNRAVL_OPT% ^
       com.sas.unravl.Main ^
       %*
