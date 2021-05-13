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

# tar.gz archive
TGZ="${INSTALLERS_DIR}/apacheds-${project.version}.tar.gz"
if [ -f ${TGZ} ]
then
    echo
    echo
    echo "Testing tar.gz archive (Debian 10, OpenJDK 8, 64bit)"
    docker run -i --rm -h myhostname \
      -v ${TGZ}:/apacheds.tar.gz \
      -v ${TEST_SCRIPTS_DIR}/archive.test:/archive.test \
      openjdk:8 bash /archive.test
fi


# zip archive
ZIP="${INSTALLERS_DIR}/apacheds-${project.version}.zip"
if [ -f ${ZIP} ]
then
    echo
    echo
    echo "Testing zip archive (Debian 10, OpenJDK 11, 64bit)"
    docker run -i --rm -h myhostname \
      -v ${ZIP}:/apacheds.zip \
      -v ${TEST_SCRIPTS_DIR}/archive.test:/archive.test \
      openjdk:11 bash /archive.test
fi
