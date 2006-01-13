#!/bin/sh
#
# A standalone shell script to start apacheds-main.jar .
#
# The following VARS are parsed and replaced by ant via the maven goal 'standalone'.
#
# @..@ indicates that ant will parse this file and set the correct value.
#

MAIN_JAR="@APACHE_DS_MAIN_JAR@"

ARGS="-Xms128m -Xmx256m -jar $MAIN_JAR"

export CLASSPATH=$CLASSPATH:.

if [[ $# -eq 0 ]]
        then echo "Loading default configuration ..."
        ARGS="$ARGS apacheds-server.xml"
else
    if [[ -e $1 ]]
            if [[ ! -f $1 ]]
                    then
                        echo "Could not identify $1 as a regular file. Please check it again."
                        exit 1
            fi
        then
            CONFIG_FILE=$1
            ARGS="$ARGS $CONFIG_FILE"
            echo "Loading specified configuration: $CONFIG_FILE ..."
    fi
fi

#create the logs dir. it does not already exist
if [[ ! -d "logs" ]]
    then mkdir logs
fi

echo "java $ARGS"
java $ARGS

