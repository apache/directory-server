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

# Loading functions
. ./functions.sh

# Reading variables file and asking questions
lines=`wc -l < ./variables.sh`
count=1
lines=`expr ${lines:-0} + 1`
while [ $count -lt $lines ]
do
    ask_param $count
    count=`expr ${count:-0} + 1`
done

#
# Starting installation
#

# Verifying the user is root
#if ( test `id -un` != "root" )
#then
#    echo "Only root can install this software."
#    echo "Apache DS installation has failed."
#    exit 1 ;
#fi

# Installing
echo "Installing..."

# Copying the server files
mkdir -p $APACHEDS_HOME_DIRECTORY
verifyExitCode
cp -r ../rootFolder/server/* $APACHEDS_HOME_DIRECTORY
verifyExitCode

# Creating instances home directory
mkdir -p $INSTANCES_HOME_DIRECTORY
verifyExitCode

# Creating the default instance home directory
DEFAULT_INSTANCE_HOME_DIRECTORY=$INSTANCES_HOME_DIRECTORY/$DEFAULT_INSTANCE_NAME
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/conf
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/ldif
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/log
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/partitions
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/run
verifyExitCode

# Copying the default instance files
cp ../rootFolder/instance/apacheds.conf $DEFAULT_INSTANCE_HOME_DIRECTORY/conf/
verifyExitCode
cp ../rootFolder/instance/log4j.properties $DEFAULT_INSTANCE_HOME_DIRECTORY/conf/
verifyExitCode
#cp ../rootFolder/instance/server.xml $DEFAULT_INSTANCE_HOME_DIRECTORY/conf/
#verifyExitCode

# Filtering and copying the init.d script
sed -e "s;@APACHEDS.HOME@;${APACHEDS_HOME_DIRECTORY};" ../rootFolder/instance/apacheds-init > ../rootFolder/instance/apacheds-init.tmp
verifyExitCode
mv ../rootFolder/instance/apacheds-init.tmp ../rootFolder/instance/apacheds-init
verifyExitCode
sed -e "s;@INSTANCE.HOME@;${INSTANCES_HOME_DIRECTORY};" ../rootFolder/instance/apacheds-init > ../rootFolder/instance/apacheds-init.tmp
verifyExitCode
mv ../rootFolder/instance/apacheds-init.tmp ../rootFolder/instance/apacheds-init
verifyExitCode
sed -e "s;@INSTANCE@;${DEFAULT_INSTANCE_NAME};" ../rootFolder/instance/apacheds-init > ../rootFolder/instance/apacheds-init.tmp
verifyExitCode
mv ../rootFolder/instance/apacheds-init.tmp ../rootFolder/instance/apacheds-init
verifyExitCode
sed -e "s;@RUN_AS_USER@;${RUN_AS_USER};" ../rootFolder/instance/apacheds-init > ../rootFolder/instance/apacheds-init.tmp
verifyExitCode
mv ../rootFolder/instance/apacheds-init.tmp ../rootFolder/instance/apacheds-init
verifyExitCode
cp ../rootFolder/instance/apacheds-init $STARTUP_SCRIPT_DIRECTORY/apacheds-$APACHEDS_VERSION-$DEFAULT_INSTANCE_NAME
verifyExitCode

# Setting the correct permissions on executable files
chmod +x $STARTUP_SCRIPT_DIRECTORY/apacheds-$APACHEDS_VERSION-$DEFAULT_INSTANCE_NAME
verifyExitCode
chmod +x $APACHEDS_HOME_DIRECTORY/bin/apacheds
verifyExitCode

# Creating the apacheds user (only if needed)
USER=`eval "id -u -n $RUN_AS_USER"`
if [ ! "X$RUN_AS_USER" = "X$USER" ]
then
	/usr/sbin/groupadd $RUN_AS_USER >/dev/null 2>&1 || :
	verifyExitCode
	/usr/sbin/useradd -g $RUN_AS_USER -d $APACHEDS_HOME_DIRECTORY $RUN_AS_USER >/dev/null 2>&1 || :
	verifyExitCode
fi

# Modifying owner
chown -R $RUN_AS_USER:$RUN_AS_USER $APACHEDS_HOME_DIRECTORY
chown -R $RUN_AS_USER:$RUN_AS_USER $INSTANCES_HOME_DIRECTORY
chown $RUN_AS_USER:$RUN_AS_USER $STARTUP_SCRIPT_DIRECTORY/apacheds-$APACHEDS_VERSION-$DEFAULT_INSTANCE_NAME
