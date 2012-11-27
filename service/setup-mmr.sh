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

# A script to setup MMR between two server instances

JAR=`find . -name "apacheds-service-*[!s].jar"`

# Checking if the final jar exists
if [ -e $JAR ] ; then
  echo "Service jar exists"
else
  echo "Service jar not found, need to build it"
  mvn clean install
fi

#copies the apache DS jar file to two different instances
PEER1=/tmp/peer1
CONF1=$PEER1/target/instance/conf
mkdir -p $PEER1
mkdir -p $CONF1
cp $JAR $PEER1/target
cp apacheds.sh $PEER1
cp log4j.properties $PEER1
cp ../server-config/src/main/resources/config.ldif $CONF1
sed -i -e "s/ads-systemport:\ 10389/ads-systemport:\ 16389/" $CONF1/config.ldif
sed -i -e "s/ads-systemport:\ 10636/ads-systemport:\ 16636/" $CONF1/config.ldif
echo >> $CONF1/config.ldif
cat src/test/resources/peer1.ldif >> $CONF1/config.ldif

PEER2=/tmp/peer2
CONF2=$PEER2/target/instance/conf
mkdir -p $PEER2
mkdir -p $CONF2
cp $JAR $PEER2/target
cp apacheds.sh $PEER2
cp log4j.properties $PEER2
cp ../server-config/src/main/resources/config.ldif $CONF2
sed -i -e "s/ads-systemport:\ 10389/ads-systemport:\ 17389/" $CONF2/config.ldif
sed -i -e "s/ads-systemport:\ 10636/ads-systemport:\ 17636/" $CONF2/config.ldif
echo >> $CONF2/config.ldif
cat src/test/resources/peer2.ldif >> $CONF2/config.ldif
