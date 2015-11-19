#!/bin/bash

# Run UnRAVL's main entry point.
# Command line arguments are UnRAVL script files to execute:
# unravl script1.json script2.json script3.json

UNRAVL_DIR=`dirname $0`/../../..
UNRAVL_JAR_DIR="$UNRAVL_DIR/build/libs"
UNRAVL_LIB_DIR="$UNRAVL_DIR/build/output/lib"
if [ -x "$JAVA_HOME/bin/java" ]
then java="$JAVA_HOME/bin/java"
else java=java
fi

$java -Dapp.name=UnRAVL \
      -classpath "$UNRAVL_JAR_DIR/*:$UNRAVL_LIB_DIR/*" \
      $UNRAVL_OPT \
      com.sas.unravl.Main \
      "$@"
