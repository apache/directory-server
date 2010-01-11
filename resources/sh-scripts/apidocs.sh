#!/bin/sh

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



OUTPUTDIR='target/site/docs/api'

SOURCEPATH=$(find . -maxdepth 4 -name src | awk '{ print $1"/main/java" }' | tr '\n' ':')

PACKAGES='org.apache.directory.server'

#EXCLUDES="org.apache.mina.examples:$(grep -h '^package org\.apache\.mina.*support;$' * -R | sed 's/^package \(.*\.support\);/\1/g' | sed 's/\r//g' | tr '\n' ':' | sed 's/:$//g'| sort -u)"

javadoc -d $OUTPUTDIR -sourcepath $SOURCEPATH -subpackages $PACKAGES #-exclude $EXCLUDES 
