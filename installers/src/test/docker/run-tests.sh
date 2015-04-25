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

# Debian installer 64bit
DEB64="${project.build.directory}/installers/apacheds-${project.version}-amd64.deb"
if [ -f ${DEB64} ]
then
    # Run deb installer with official Java image (Debian 8, OpenJDK 8, 64bit):
    docker run -i --rm \
      -v ${DEB64}:/apacheds.deb \
      -v ${project.build.directory}/docker/deb.test:/deb.test \
      java:8 bash /deb.test

    # Run deb installer with 'dockerfile' Java image (Ubuntu 14.04, Oracle Java 7, 64bit):
    docker run -i --rm \
      -v ${DEB64}:/apacheds.deb \
      -v ${project.build.directory}/docker/deb.test:/deb.test \
      dockerfile/java:oracle-java7 bash /deb.test
fi

# Binary Installer 64bit
BIN64="${project.build.directory}/installers/apacheds-${project.version}-64bit.bin"
if [ -f ${BIN64} ]
then
    # Run bin installer with official Java image (Debian 8, OpenJDK 8, 64bit):
    docker run -i --rm \
      -v ${BIN64}:/apacheds.bin \
      -v ${project.build.directory}/docker/bin.test:/bin.test \
      java:8 bash /bin.test
fi

# RPM installer 64bit
RPM64="${project.build.directory}/installers/apacheds-${project.version}-x86_64.rpm"
if [ -f ${RPM64} ]
then
    # Run rpm installer with official Fedora (Fedora 21, OpenJDK 8)
    #docker run -i --rm \
    #  -v ${RPM64}:/apacheds.rpm \
    #  -v ${project.build.directory}/docker/rpm.test:/rpm.test \
    #  fedora:latest bash /rpm.test

    # Run rpm installer with official Centos (CentOS 7, OpenJDK 8)
    docker run -i --rm \
      -v ${RPM64}:/apacheds.rpm \
      -v ${project.build.directory}/docker/rpm.test:/rpm.test \
      centos:7 bash /rpm.test
fi

