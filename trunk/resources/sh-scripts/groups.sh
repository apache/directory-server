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

RESOURCES=../../resources

for project in *
do
	cd $project &> /dev/null
	if [ $? -ne 0 ]
	then
		continue
	fi
	if [ ! -d "src/main/java" ]
	then
		cd - &> /dev/null
		continue
	fi
	ROOTPACKAGENAME=$(grep -h '^package org\.apache\.directory\.server.*;$' * -R | sed 's/\r//g' | sort -u | head -1 | sed 's/\(package \)//' | tr -d ';')
	if [ ! -f pom.xml ]
	then
		cd - &> /dev/null
		continue
	fi
	NAME=$($RESOURCES/pomutils/name.sh ./pom.xml)

	echo $ROOTPACKAGENAME
	echo $NAME
	echo "----------------------------------------------------------"
	cd - &> /dev/null
done
