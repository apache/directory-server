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

# Binary installer 64bit
BIN64="${INSTALLERS_DIR}/apacheds-${project.version}-64bit.bin"
if [ -f ${BIN64} ]
then
    echo
    echo
    echo "Testing bin installer (Debian 10, OpenJDK 8, 64bit)"
    docker run -i --rm -h myhostname \
      -v ${BIN64}:/apacheds.bin \
      -v ${TEST_SCRIPTS_DIR}/bin.test:/bin.test \
      openjdk:8 bash /bin.test

    echo
    echo
    echo "Testing bin installer (DIRSERVER-2173) (Debian 10, OpenJDK 8, 64bit)"
    docker run -i --rm -h myhostname \
      -v ${BIN64}:/apacheds.bin \
      -v ${TEST_SCRIPTS_DIR}/bin-DIRSERVER-2173.test:/bin-DIRSERVER-2173.test \
      openjdk:8 bash /bin-DIRSERVER-2173.test
fi
