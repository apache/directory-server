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

Where do you want to install ApacheDS|APACHEDS_HOME_DIRECTORY|/opt/apacheds-${APACHEDS_VERSION}||
Where do you want to install ApacheDS instances|INSTANCES_HOME_DIRECTORY|/var/lib/apacheds-${APACHEDS_VERSION}||
What name do you want for the default instance|DEFAULT_INSTANCE_NAME|default||
Where do you want to install the startup script|STARTUP_SCRIPT_DIRECTORY|/etc/init.d||
Which user do you want to run the server with (if not already existing, the specified user will be created)|RUN_AS_USER|apacheds||
Which group do you want to run the server with (if not already existing, the specified group will be created)|RUN_AS_GROUP|apacheds||
