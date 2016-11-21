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

# Debian package 64bit
DEB64="${project.build.directory}/installers/apacheds-${project.version}-amd64.deb"
if [ -f ${DEB64} ]
then
    echo
    echo
    echo "Testing debian package with official Java image (Debian 8, OpenJDK 8, 64bit)"
    docker run -i --rm \
      -v ${DEB64}:/apacheds.deb \
      -v ${project.build.directory}/docker/deb.test:/deb.test \
      java:8 bash /deb.test

    echo
    echo
    echo "Testing debian package with 'nimmis' Java image (Ubuntu 14.04, Oracle Java 7, 64bit)"
    docker run -i --rm \
      -v ${DEB64}:/apacheds.deb \
      -v ${project.build.directory}/docker/deb.test:/deb.test \
      nimmis/java:oracle-7-jdk bash /deb.test
fi


# RPM package 64bit
RPM64="${project.build.directory}/installers/apacheds-${project.version}-x86_64.rpm"
if [ -f ${RPM64} ]
then
    echo
    echo
    echo "Testing RPM package with official Fedora image (Fedora 21, OpenJDK 8)"
    docker run -i --rm \
      -v ${RPM64}:/apacheds.rpm \
      -v ${project.build.directory}/docker/rpm.test:/rpm.test \
      fedora:21 bash /rpm.test

    echo
    echo
    echo "Testing RPM package with official Centos image (CentOS 7, OpenJDK 8, 64bit)"
    docker run -i --rm \
      -v ${RPM64}:/apacheds.rpm \
      -v ${project.build.directory}/docker/rpm.test:/rpm.test \
      centos:7 bash /rpm.test
fi


# Binary installer 64bit
BIN64="${project.build.directory}/installers/apacheds-${project.version}-64bit.bin"
if [ -f ${BIN64} ]
then
    echo
    echo
    echo "Testing bin installer with official Java image (Debian 8, OpenJDK 8, 64bit)"
    docker run -i --rm \
      -v ${BIN64}:/apacheds.bin \
      -v ${project.build.directory}/docker/bin.test:/bin.test \
      java:8 bash /bin.test

    echo
    echo
    echo "Testing bin installer (DIRSERVER-2173) with official Java image (Debian 8, OpenJDK 8, 64bit)"
    docker run -i --rm \
      -v ${BIN64}:/apacheds.bin \
      -v ${project.build.directory}/docker/bin2.test:/bin2.test \
      java:8 bash /bin2.test
fi


# tar.gz archive
TGZ="${project.build.directory}/installers/apacheds-${project.version}.tar.gz"
if [ -f ${TGZ} ]
then
    echo
    echo
    echo "Testing tar.gz archive with official Java image (Debian 8, OpenJDK 8, 64bit)"
    docker run -i --rm \
      -v ${TGZ}:/apacheds.tar.gz \
      -v ${project.build.directory}/docker/archive.test:/archive.test \
      java:8 bash /archive.test
fi


# zip archive
ZIP="${project.build.directory}/installers/apacheds-${project.version}.zip"
if [ -f ${ZIP} ]
then
    echo
    echo
    echo "Testing zip archive with 'nimmis' Java image (Ubuntu 14.04, Oracle Java 7, 64bit)"
    docker run -i --rm \
      -v ${ZIP}:/apacheds.zip \
      -v ${project.build.directory}/docker/archive.test:/archive.test \
      nimmis/java:oracle-7-jdk bash /archive.test
fi
