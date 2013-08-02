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
JAR=`find . -name "apacheds-service-*[!s].jar"`

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

ADS_CONTROLS="-Dapacheds.controls=org.apache.directory.api.ldap.codec.controls.cascade.CascadeFactory,org.apache.directory.api.ldap.codec.controls.manageDsaIT.ManageDsaITFactory,org.apache.directory.api.ldap.codec.controls.search.entryChange.EntryChangeFactory,org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsFactory,org.apache.directory.api.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory,org.apache.directory.api.ldap.codec.controls.search.subentries.SubentriesFactory,org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncDoneValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncInfoValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncRequestValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory"

ADS_EXTENDED_OPERATIONS="-Dapacheds.extendedOperations=org.apache.directory.api.ldap.extras.extended.ads_impl.cancel.CancelFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.certGeneration.CertGenerationFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.gracefulShutdown.GracefulShutdownFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.storedProcedure.StoredProcedureFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.gracefulDisconnect.GracefulDisconnectFactory"

java $JAVA_OPTS $ADS_CONTROLS $ADS_EXTENDED_OPERATIONS -Dlog4j.configuration=file:./target/instance/conf/log4j.properties -Dapacheds.log.dir=./target/instance/log -jar $JAR ./target/instance
