#!/bin/bash

#export SPARROW_HOME=$(dirname $(dirname $(readlink -f "$0"))) 

export SPARROW_HOME=$(pwd) #get currenty directory

cd $SPARROW_HOME

SPARROW_JAR=$SPARROW_HOME/target/sparrow-1.0-SNAPSHOT.jar
SPARROW_CLASSPATH=$SPARROW_HOME/target/lib/*
SIGAR_LIB=$SPARROW_HOME/sigar-bin
SPARROW_MAIN=org.sparrow.service.SparrowDaemon

function exec_sparrow {
	eval java -Djava.library.path="$SIGAR_LIB" -cp "'$SPARROW_JAR:$SPARROW_CLASSPATH'" $SPARROW_MAIN
}

if [[ "$1" == "build" ]]; then
	eval mvn clean install
fi

if [[ "$1" == "start" || "$1" == "" ]]; then
	exec_sparrow
fi

if [[ "$1" == "datatool" ]]; then
	SPARROW_MAIN=org.sparrow.tools.DataExtract
	exec_sparrow
fi
