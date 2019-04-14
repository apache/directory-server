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

set -e

TEST_SCRIPTS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
INSTALLERS_DIR="$TEST_SCRIPTS_DIR/../installers"

# Debian package 64bit
DEB64="${INSTALLERS_DIR}/apacheds-${project.version}-amd64.deb"
if [ -f ${DEB64} ]
then
    echo
    echo
    echo "Testing deb package (Debian 9, OpenJDK 8, 64bit)"
    docker run -i --rm \
      -v ${DEB64}:/apacheds.deb \
      -v ${TEST_SCRIPTS_DIR}/deb.test:/deb.test \
      debian:9 bash /deb.test

    echo
    echo
    echo "Testing deb package (Ubuntu 18.04, OpenJDK 10, 64bit)"
    docker run -i --rm \
      -v ${DEB64}:/apacheds.deb \
      -v ${TEST_SCRIPTS_DIR}/deb.test:/deb.test \
      ubuntu:18.04 bash /deb.test

    echo
    echo
    echo "Testing deb package (Ubuntu 18.04, OpenJ9 12, 64bit)"
    docker run -i --rm \
      -v ${DEB64}:/apacheds.deb \
      -v ${TEST_SCRIPTS_DIR}/deb.test:/deb.test \
      adoptopenjdk/openjdk12-openj9:slim bash -c "ln -s /opt/java/openjdk/bin/java /usr/bin/java; bash /deb.test"
fi
