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

# RPM package 64bit
RPM64="${INSTALLERS_DIR}/apacheds-${project.version}-x86_64.rpm"
DOCKER_CMD="docker run -i --rm -h myhostname -v ${RPM64}:/apacheds.rpm -v ${TEST_SCRIPTS_DIR}/rpm.test:/rpm.test"
if [ -f ${RPM64} ]
then
    echo
    echo
    echo "Testing rpm package (Centos 7, OpenJDK 8, 64bit)"
    $DOCKER_CMD centos:7 bash /rpm.test

    echo
    echo
    echo "Testing rpm package (Fedora 34, OpenJDK latest (16+), 64bit)"
    $DOCKER_CMD fedora:34 bash /rpm.test

    echo
    echo
    echo "Testing rpm package (Amazon Corretto 11, 64bit)"
    $DOCKER_CMD amazoncorretto:11 bash /rpm.test
fi
