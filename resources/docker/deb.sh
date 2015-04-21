#!/bin/bash

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

# 
# Script to test Debian installer within Docker container.
#
# Run with official Java image (Debian 8, OpenJDK 8)
#
#     cat deb.sh | docker run -i --rm -e URL="<URL to .deb>" -e VERSION="<ApacheDS version>" java:8
#
# Run with 'dockerfile' Java image (Ubuntu 14.04, Oracle Java 7)
#
#     cat deb.sh | docker run -i --rm -e URL="<URL to .deb>" -e VERSION="<ApacheDS version>" dockerfile/java:oracle-java7
#


NAME="apacheds-${VERSION}"
DIRNAME="${NAME}"
SERVICE_NAME="${NAME}-default"


# stop execution if any command fails (i.e. exits with status code > 0)
set -e

# trace commands
set -x

# download
wget -O apacheds.deb ${URL}

# install
dpkg -i apacheds.deb

# assert installed
dpkg -l | grep apacheds

# assert files and directories exist
test -f /opt/${DIRNAME}/LICENSE
test -f /opt/${DIRNAME}/NOTICE
test -d/var/lib/${DIRNAME}/default

# assert not running
service ${SERVICE_NAME} status | grep "ApacheDS - default is not running"

# start
service ${SERVICE_NAME} start 
sleep 5

# assert running
service ${SERVICE_NAME} status | grep "ApacheDS - default is running"

# install ldapsearch
apt-get update && apt-get install -y ldap-utils

# search
ldapsearch -h localhost -p 10389 -x -D "uid=admin,ou=system" -w secret -s base -b "dc=example,dc=com"

# restart
service ${SERVICE_NAME} restart 
sleep 5

# search after restart
ldapsearch -h localhost -p 10389 -x -D "uid=admin,ou=system" -w secret -s base -b "dc=example,dc=com"

# stop
service ${SERVICE_NAME} stop

# assert not running
service ${SERVICE_NAME} status | grep "ApacheDS - default is not running"

# uninstall
dpkg -P apacheds

# assert files and directory no more exist
test ! -e /opt/${DIRNAME}
 
# SUCCESS
echo "SUCCESS"

