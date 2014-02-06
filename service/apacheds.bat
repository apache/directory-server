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

REM IF NOT exist target/apacheds-service-*.jar GOTO :MVN
IF NOT exist target/apacheds-service-2.0.0-M11.jar GOTO :MVN
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
  
set ADS_CONTROLS="-Dapacheds.controls="

set ADS_EXTENDED_OPERATIONS="-Dapacheds.extendedOperations="

java %ADS_CONTROLS% %ADS_EXTENDED_OPERATIONS% -Dlog4j.configuration="file:///%cd%/target/instance/conf/log4j.properties" -jar target/apacheds-service-2.0.0-M11.jar %cd%/target/instance
   
