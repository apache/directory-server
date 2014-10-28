#!/bin/sh
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

# Detect ads home (http://stackoverflow.com/a/630387/516433)
ADS_HOME="`dirname \"$0\"`"
ADS_HOME="`(cd \"$ADS_HOME/..\" && pwd)`"
if [ -z "$ADS_HOME" ]; then
    exit 1
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

# For cygwin, ensure paths or in unix format before touched
if $cygwin; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$ADS_HOME" ] && ADS_HOME=`cygpath --unix "$ADS_HOME"`
    [ -n "$ADS_INSTANCES" ] && ADS_INSTANCES=`cygpath --unix "$ADS_INSTANCES"`
fi

RUN_JAVA=
if [ -z "$JAVA_HOME" ]; then
    RUN_JAVA=$(which java)
else 
    RUN_JAVA=$JAVA_HOME/bin/java
fi

# Build the classpath (http://stackoverflow.com/a/4729899/516433)
CLASSPATH=$(JARS=("$ADS_HOME"/lib/*.jar); IFS=:; echo "${JARS[*]}")

ADS_INSTANCE=
if [ -z "$ADS_INSTANCES" ]; then
    ADS_INSTANCE="$ADS_HOME/instances/$ADS_INSTANCE_NAME"
else
    ADS_INSTANCE="$ADS_INSTANCES/$ADS_INSTANCE_NAME"
fi

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

ADS_CONTROLS="-Dapacheds.controls=org.apache.directory.api.ldap.codec.controls.cascade.CascadeFactory,org.apache.directory.api.ldap.codec.controls.manageDsaIT.ManageDsaITFactory,org.apache.directory.api.ldap.codec.controls.search.entryChange.EntryChangeFactory,org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsFactory,org.apache.directory.api.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory,org.apache.directory.api.ldap.codec.controls.search.subentries.SubentriesFactory,org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncDoneValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncInfoValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncRequestValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory"

ADS_EXTENDED_OPERATIONS="-Dapacheds.extendedOperations=org.apache.directory.api.ldap.extras.extended.ads_impl.cancel.CancelFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.certGeneration.CertGenerationFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.gracefulShutdown.GracefulShutdownFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.storedProcedure.StoredProcedureFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.gracefulDisconnect.GracefulDisconnectFactory"

if [ "$ADS_ACTION" = "start" ]; then
    # Printing instance information
    echo "Starting ApacheDS instance '$ADS_INSTANCE_NAME'..."

    # Launching ApacheDS
    eval "\"$RUN_JAVA\"" $JAVA_OPTS $ADS_CONTROLS $ADS_EXTENDED_OPERATIONS \
        -Dlog4j.configuration="\"file:$ADS_INSTANCE/conf/log4j.properties\"" \
        -Dapacheds.log.dir="\"$ADS_INSTANCE/log\"" \
        -classpath "\"$CLASSPATH\"" \
        org.apache.directory.server.UberjarMain "\"$ADS_INSTANCE\"" \
        > "$ADS_OUT" 2>&1 "&"
    echo $! > "$ADS_PID"
elif [ "$ADS_ACTION" = "run" ]; then
    # Printing instance information
    echo "Running ApacheDS instance '$ADS_INSTANCE_NAME'..."

    # Launching ApacheDS
    eval "\"$RUN_JAVA\"" $JAVA_OPTS $ADS_CONTROLS $ADS_EXTENDED_OPERATIONS \
        -Dlog4j.configuration="\"file:$ADS_INSTANCE/conf/log4j.properties\"" \
        -Dapacheds.log.dir="\"$ADS_INSTANCE/log\"" \
        -classpath "\"$CLASSPATH\"" \
        org.apache.directory.server.UberjarMain "\"$ADS_INSTANCE\""
elif [ "$ADS_ACTION" = "stop" ]; then
    # Printing instance information
    PID=`cat $ADS_PID`
    echo "Stoping ApacheDS instance '$ADS_INSTANCE_NAME' running as $PID"

    kill $PID
fi
