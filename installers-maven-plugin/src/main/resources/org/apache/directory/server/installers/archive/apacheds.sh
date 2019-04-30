#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

# -----------------------------------------------------------------------------
# Control Script for the ApacheDS Server
#
# Environment Variable Prerequisites
#
#   Do not set the variables in this script. Instead put them into 
#   $ADS_HOME/bin/setenv.sh to keep your customizations separate.
#
#   ADS_HOME        (Optional) The directory that contains your apacheds 
#                   install.  Defaults to the parent directory of the
#                   directory containing this script.
#
#   ADS_INSTANCES   (Optional) The parent directory for the instances.
#                   Defaults to $ADS_HOME/instances.
#
#   ADS_CONTROLS    Controls to register.
#
#   ADS_EXTENDED_OPERATIONS
#                   Extended operations to register.
#
#   ADS_INTERMEDIATE_RESPONSES
#                   Intermediate responses to register.
#
#   ADS_SHUTDOWN_PORT
#                   (Optional) If specified, it must be a valid port number
#                   between 1024 and 65536 on which ApacheDS will listen for 
#                   a connection to trigger a polite shutdown.  Defaults to 0
#                   indicating a dynamic port allocation.
#
#   JAVA_HOME       (Optional) The java installation directory.  If not
#                   not specified, the java from $PATH will be used.
#
#   JAVA_OPTS       (Optional) Any additional java options (ex: -Xms:256m)

# Defaults
ADS_SHUTDOWN_PORT=0

# Detect ads home (http://stackoverflow.com/a/630387/516433)
PROGRAM_DIR="`dirname \"$0\"`"
[ -z "$ADS_HOME" ] && ADS_HOME="`(cd \"$PROGRAM_DIR/..\" && pwd)`"
if [ -z "$ADS_HOME" ]; then
    echo "Unable to detect ADS_HOME, and not specified"
    exit 1
fi

HAVE_TTY=0
if [ "`tty`" != "not a tty" ]; then
    HAVE_TTY=1
fi

# OS sepecific support
cygwin=false
case "`uname`" in
    CYGWIN*) cygwin=true
esac

# Checking the parameters
ADS_INSTANCE_NAME=
ADS_ACTION=
if [ $# -eq 1 ]
then
    # Using 'default' as default instance name
    ADS_INSTANCE_NAME="default"
    ADS_ACTION=$1
elif [ $# -eq 2 ]
then
    # Getting the instance name from the arguments
    ADS_INSTANCE_NAME=$1
    ADS_ACTION=$2
else
    # Printing usage information
    echo "Usage: apacheds.sh [<instance name>] <action>"
    echo "If <instance name> is ommited, 'default' will be used."
    echo "<action> is one of start, stop."
    exit 1
fi

[ -r "$ADS_HOME/bin/setenv.sh" ] && . "$ADS_HOME/bin/setenv.sh"

# For cygwin, ensure paths or in unix format before touched
if $cygwin; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$ADS_HOME" ] && ADS_HOME=`cygpath --unix "$ADS_HOME"`
    [ -n "$ADS_INSTANCES" ] && ADS_INSTANCES=`cygpath --unix "$ADS_INSTANCES"`
fi

[ -z "$ADS_INSTANCES" ] && ADS_INSTANCES="$ADS_HOME/instances"

RUN_JAVA=
if [ -z "$JAVA_HOME" ]; then
    RUN_JAVA=$(which java)
else 
    RUN_JAVA=$JAVA_HOME/bin/java
fi

# Build the classpath (http://stackoverflow.com/a/4729899/516433)
CLASSPATH=$(JARS=("$ADS_HOME"/lib/*.jar); IFS=:; echo "${JARS[*]}")

ADS_INSTANCE="$ADS_INSTANCES/$ADS_INSTANCE_NAME"

ADS_OUT="$ADS_INSTANCE/log/apacheds.out"
ADS_PID="$ADS_INSTANCE/run/apacheds.pid"

# For cygwin, switch to windows paths before running
if $cygwin; then
    JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
    ADS_HOME=`cygpath --absolute --windows "$ADS_HOME"`
    ADS_INSTANCES=`cygpath --absolute --windows "$ADS_INSTANCES"`
    ADS_INSTANCE=`cygpath --absolute --windows "$ADS_INSTANCE"`
    CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

[ -z "$ADS_CONTROLS" ] && ADS_CONTROLS="-Dapacheds.controls="

[ -z "$ADS_EXTENDED_OPERATIONS" ] && ADS_EXTENDED_OPERATIONS="-Dapacheds.extendedOperations="

[ -z "$ADS_INTERMEDIATE_RESPONSES" ] && ADS_INTERMEDIATE_RESPONSES="-Dapacheds.intermediateResponses="

if [ $HAVE_TTY -eq 1 ]; then
    echo "Using ADS_HOME:    $ADS_HOME"
    echo "Using JAVA_HOME:   $JAVA_HOME"
    echo ""
fi

if [ "$ADS_ACTION" = "start" ]; then
    # Printing instance information
    [ $HAVE_TTY -eq 1 ] && echo "Starting ApacheDS instance '$ADS_INSTANCE_NAME'..."

    if [ -f $ADS_PID ]; then
        PID=`cat $ADS_PID`
        if kill -0 $PID > /dev/null 2>&1; then
            echo "ApacheDS is already running as $PID"
            exit 0
        fi
    fi

    # Launching ApacheDS
    eval "\"$RUN_JAVA\"" $JAVA_OPTS $ADS_CONTROLS $ADS_EXTENDED_OPERATIONS $ADS_INTERMEDIATE_RESPONSES \
        -Dlog4j.configuration="\"file:$ADS_INSTANCE/conf/log4j.properties\"" \
        -Dapacheds.shutdown.port="\"$ADS_SHUTDOWN_PORT\"" \
        -Dapacheds.log.dir="\"$ADS_INSTANCE/log\"" \
        -classpath "\"$CLASSPATH\"" \
        org.apache.directory.server.UberjarMain "\"$ADS_INSTANCE\"" \
        > "$ADS_OUT" 2>&1 "&"
    echo $! > "$ADS_PID"
elif [ "$ADS_ACTION" = "repair" ]; then
    # Printing instance information
    [ $HAVE_TTY -eq 1 ] && echo "Repairing ApacheDS instance '$ADS_INSTANCE_NAME'..."

    if [ -f $ADS_PID ]; then
        PID=`cat $ADS_PID`
        if kill -0 $PID > /dev/null 2>&1; then
            echo "ApacheDS is already running as $PID"
            exit 0
        fi
    fi

    # Repairing ApacheDS
    eval "\"$RUN_JAVA\"" $JAVA_OPTS $ADS_CONTROLS $ADS_EXTENDED_OPERATIONS $ADS_INTERMEDIATE_RESPONSES \
        -Dlog4j.configuration="\"file:$ADS_INSTANCE/conf/log4j.properties\"" \
        -Dapacheds.shutdown.port="\"$ADS_SHUTDOWN_PORT\"" \
        -Dapacheds.log.dir="\"$ADS_INSTANCE/log\"" \
        -classpath "\"$CLASSPATH\"" \
        org.apache.directory.server.UberjarMain "\"$ADS_INSTANCE\"" repair
        org.apache.directory.server.UberjarMain "\"$ADS_INSTANCE\"" repair
    
    # Printing instance information
    [ $HAVE_TTY -eq 1 ] && echo "Starting ApacheDS instance '$ADS_INSTANCE_NAME'..."

    # Launching ApacheDS
    eval "\"$RUN_JAVA\"" $JAVA_OPTS $ADS_CONTROLS $ADS_EXTENDED_OPERATIONS $ADS_INTERMEDIATE_RESPONSES \
        -Dlog4j.configuration="\"file:$ADS_INSTANCE/conf/log4j.properties\"" \
        -Dapacheds.shutdown.port="\"$ADS_SHUTDOWN_PORT\"" \
        -Dapacheds.log.dir="\"$ADS_INSTANCE/log\"" \
        -classpath "\"$CLASSPATH\"" \
        org.apache.directory.server.UberjarMain "\"$ADS_INSTANCE\"" \
        > "$ADS_OUT" 2>&1 "&"
    echo $! > "$ADS_PID"
elif [ "$ADS_ACTION" = "run" ]; then
    # Printing instance information
    [ $HAVE_TTY -eq 1 ] && echo "Running ApacheDS instance '$ADS_INSTANCE_NAME'..."

    # Launching ApacheDS
    eval "\"$RUN_JAVA\"" $JAVA_OPTS $ADS_CONTROLS $ADS_EXTENDED_OPERATIONS $ADS_INTERMEDIATE_RESPONSES \
        -Dlog4j.configuration="\"file:$ADS_INSTANCE/conf/log4j.properties\"" \
        -Dapacheds.log.dir="\"$ADS_INSTANCE/log\"" \
        -Dapacheds.shutdown.port="\"$ADS_SHUTDOWN_PORT\"" \
        -classpath "\"$CLASSPATH\"" \
        org.apache.directory.server.UberjarMain "\"$ADS_INSTANCE\""
elif [ "$ADS_ACTION" = "status" ]; then
    if [ -f $ADS_PID ]; then
        PID=`cat $ADS_PID`
        if kill -0 $PID > /dev/null 2>&1; then
            echo "ApacheDS is running as $PID"
        else
            echo "ApacheDS is not running"
        fi
    else
        [ $HAVE_TTY -eq 1 ] && echo "ApacheDS is not running"
    fi
elif [ "$ADS_ACTION" = "stop" ]; then
    # Printing instance information
    if [ -f $ADS_PID ]; then
        PID=`cat $ADS_PID`
        [ $HAVE_TTY -eq 1 ] && echo "Stopping ApacheDS instance '$ADS_INSTANCE_NAME' running as $PID"

        # Terminate the process
        if [ $ADS_SHUTDOWN_PORT -ge 0 ]; then
            eval "\"$RUN_JAVA\"" $JAVA_OPTS $ADS_CONTROLS $ADS_EXTENDED_OPERATIONS $ADS_INTERMEDIATE_RESPONSES \
                -Dlog4j.configuration="\"file:$ADS_INSTANCE/conf/log4j.properties\"" \
                -Dapacheds.log.dir="\"$ADS_INSTANCE/log\"" \
                -Dapacheds.shutdown.port="\"$ADS_SHUTDOWN_PORT\"" \
                -classpath "\"$CLASSPATH\"" \
                org.apache.directory.server.UberjarMain "\"$ADS_INSTANCE\"" stop
        else
            # No port specified so try term signal instead
            kill -15 $PID > /dev/null 2>&1
        fi

        ATTEMPTS_REMAINING=60
        while [ $ATTEMPTS_REMAINING -gt 0 ]; do
            kill -0 $PID > /dev/null 2>&1
            EXIT_CODE=$?
            if [ $EXIT_CODE -gt 0 ]; then
                rm -f $ADS_PID > /dev/null 2>&1
                [ $HAVE_TTY -eq 1 ] && echo "ApacheDS instance '$ADS_INSTANCE_NAME' stopped successfully"
                exit 0
            fi
            sleep 1
            [ $HAVE_TTY -eq 1 ] && echo "ApacheDS stopping $PID: $EXIT_CODE, $ATTEMPTS_REMAINING attempts remaining"
            ATTEMPTS_REMAINING=`expr $ATTEMPTS_REMAINING - 1`
        done
        exit 1 # failed to exit successfully
    else
        [ $HAVE_TTY -eq 1 ] && echo "ApacheDS is not running, $ADS_PID does not exist"
    fi
fi
