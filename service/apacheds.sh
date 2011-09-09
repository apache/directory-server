#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# The final jar
JAR=target/apacheds-service-2.0.0-M3.jar

# Checking if the final jar exists
if [ -e $JAR ] ; then
  echo "Service jar exists"
else
  echo "Service jar not found, need to build it"
  mvn clean install
fi

# Creating the instance layout
mkdir -p target/instance
mkdir -p target/instance/conf
cp log4j.properties target/instance/conf/log4j.properties
mkdir -p target/instance/partitions
mkdir -p target/instance/log

if [ "$1" = -debug ] ; then
  echo 'remote debugging enabled in suspension mode, attach a debugger to continue execution'
  JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8008 -Xnoagent -Djava.compiler=NONE"
fi

DEF_CTRLS="-Ddefault.controls=org.apache.directory.shared.ldap.codec.controls.cascade.CascadeFactory,org.apache.directory.shared.ldap.codec.controls.manageDsaIT.ManageDsaITFactory,org.apache.directory.shared.ldap.codec.controls.search.entryChange.EntryChangeFactory,org.apache.directory.shared.ldap.codec.controls.search.pagedSearch.PagedResultsFactory,org.apache.directory.shared.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory,org.apache.directory.shared.ldap.codec.controls.search.subentries.SubentriesFactory"

EXT_CTRLS="-Dextra.controls=org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyFactory,org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncDoneValueFactory,org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncInfoValueFactory,org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncRequestValueFactory,org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory"

DEF_EXT_OP_REQ="-Ddefault.extendedOperation.requests=org.apache.directory.shared.ldap.extras.extended.ads_impl.cancel.CancelFactory,org.apache.directory.shared.ldap.extras.extended.ads_impl.certGeneration.CertGenerationFactory,org.apache.directory.shared.ldap.extras.extended.ads_impl.gracefulShutdown.GracefulShutdownFactory,org.apache.directory.shared.ldap.extras.extended.ads_impl.storedProcedure.StoredProcedureFactory"

DEF_EXT_OP_RESP="-Ddefault.extendedOperation.responses=org.apache.directory.shared.ldap.extras.extended.ads_impl.gracefulDisconnect.GracefulDisconnectFactory"

java $JAVA_OPTS $DEF_CTRLS $EXT_CTRLS $DEF_EXT_OP_REQ $DEF_EXT_OP_RESP -Dlog4j.configuration=file:./target/instance/conf/log4j.properties -Dapacheds.log.dir=./target/instance/log -jar $JAR ./target/instance
