@echo off

REM Licensed to the Apache Software Foundation (ASF) under one
REM or more contributor license agreements.  See the NOTICE file
REM distributed with this work for additional information
REM regarding copyright ownership.  The ASF licenses this file
REM to you under the Apache License, Version 2.0 (the
REM "License"); you may not use this file except in compliance
REM with the License.  You may obtain a copy of the License at
REM 
REM   http://www.apache.org/licenses/LICENSE-2.0
REM 
REM Unless required by applicable law or agreed to in writing,
REM software distributed under the License is distributed on an
REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM KIND, either express or implied.  See the License for the
REM specific language governing permissions and limitations
REM under the License. 

REM this script starts ApacheDS (as non-windows-service)

IF NOT exist target/apacheds-service-2.0.0-M6.jar GOTO :MVN
   echo "Service jar exists"
   GOTO :JAVA

:MVN
   echo "Service jar not found, need to build it"
   call mvn clean install
   GOTO :JAVA

:JAVA
   md target/instance
   md target/instance/conf
   cp log4j.properties target/instance/conf/log4j.properties
   md target/instance/partitions
   md target/instance/log
  
set DEF_CTRLS="-Ddefault.controls=org.apache.directory.shared.ldap.codec.controls.cascade.CascadeFactory,org.apache.directory.shared.ldap.codec.controls.manageDsaIT.ManageDsaITFactory,org.apache.directory.shared.ldap.codec.controls.search.entryChange.EntryChangeFactory,org.apache.directory.shared.ldap.codec.controls.search.pagedSearch.PagedResultsFactory,org.apache.directory.shared.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory,org.apache.directory.shared.ldap.codec.controls.search.subentries.SubentriesFactory"

set EXT_CTRLS="-Dextra.controls=org.apache.directory.shared.ldap.extras.controls.ppolicy_impl.PasswordPolicyFactory,org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncDoneValueFactory,org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncInfoValueFactory,org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncRequestValueFactory,org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory"

set DEF_EXT_OP_REQ="-Ddefault.extendedOperation.requests=org.apache.directory.shared.ldap.extras.extended.ads_impl.cancel.CancelFactory,org.apache.directory.shared.ldap.extras.extended.ads_impl.certGeneration.CertGenerationFactory,org.apache.directory.shared.ldap.extras.extended.ads_impl.gracefulShutdown.GracefulShutdownFactory,org.apache.directory.shared.ldap.extras.extended.ads_impl.storedProcedure.StoredProcedureFactory"

set DEF_EXT_OP_RESP="-Ddefault.extendedOperation.responses=org.apache.directory.shared.ldap.extras.extended.ads_impl.gracefulDisconnect.GracefulDisconnectFactory"

java %DEF_CTRLS% %EXT_CTRLS% %DEF_EXT_OP_REQ% %DEF_EXT_OP_RESP% -Dlog4j.configuration="file:///%cd%/target/instance/conf/log4j.properties" -jar target/apacheds-service-2.0.0-M6.jar %cd%/target/instance
   
