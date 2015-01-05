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
#    echo "ApacheDS installation has failed."
#    exit 1 ;
#fi

# Installing
echo "Installing..."

# Filtering apacheds script file
sed -e "s;@installation.directory@;${APACHEDS_HOME_DIRECTORY};" ../server/bin/apacheds > ../server/bin/apacheds.tmp
verifyExitCode

mv ../server/bin/apacheds.tmp ../server/bin/apacheds
verifyExitCode

sed -e "s;@instances.directory@;${INSTANCES_HOME_DIRECTORY};" ../server/bin/apacheds > ../server/bin/apacheds.tmp
verifyExitCode

mv ../server/bin/apacheds.tmp ../server/bin/apacheds
verifyExitCode

sed -e "s;@user@;${RUN_AS_USER};" ../server/bin/apacheds > ../server/bin/apacheds.tmp
verifyExitCode

mv ../server/bin/apacheds.tmp ../server/bin/apacheds
verifyExitCode

sed -e "s;@group@;${RUN_AS_GROUP};" ../server/bin/apacheds > ../server/bin/apacheds.tmp
verifyExitCode

mv ../server/bin/apacheds.tmp ../server/bin/apacheds
verifyExitCode

# Copying the server files
mkdir -p $APACHEDS_HOME_DIRECTORY
verifyExitCode
cp -r ../server/* $APACHEDS_HOME_DIRECTORY
verifyExitCode

# Creating instances home directory
mkdir -p $INSTANCES_HOME_DIRECTORY
verifyExitCode

# Creating initd directory if needed
mkdir -p $STARTUP_SCRIPT_DIRECTORY
verifyExitCode

# Creating the default instance home directory
DEFAULT_INSTANCE_HOME_DIRECTORY=$INSTANCES_HOME_DIRECTORY/$DEFAULT_INSTANCE_NAME
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/conf
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/log
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/partitions
verifyExitCode
mkdir -p $DEFAULT_INSTANCE_HOME_DIRECTORY/run
verifyExitCode

# Filtering default instance wrapper.conf file
sed -e "s;@installation.directory@;${APACHEDS_HOME_DIRECTORY};" ../instance/wrapper-instance.conf > ../instance/wrapper-instance.conf.tmp
verifyExitCode
mv ../instance/wrapper-instance.conf.tmp ../instance/wrapper-instance.conf
verifyExitCode

# Copying the default instance files
cp ../instance/config.ldif $DEFAULT_INSTANCE_HOME_DIRECTORY/conf/
verifyExitCode
cp ../instance/log4j.properties $DEFAULT_INSTANCE_HOME_DIRECTORY/conf/
verifyExitCode
cp ../instance/wrapper-instance.conf $DEFAULT_INSTANCE_HOME_DIRECTORY/conf/
verifyExitCode

# Filtering and copying the init.d script
sed -e "s;@installation.directory@;${APACHEDS_HOME_DIRECTORY};" ../instance/etc-initd-script > ../instance/etc-initd-script.tmp
verifyExitCode
mv ../instance/etc-initd-script.tmp ../instance/etc-initd-script
verifyExitCode
sed -e "s;@default.instance.name@;$DEFAULT_INSTANCE_NAME;" ../instance/etc-initd-script > ../instance/etc-initd-script.tmp
verifyExitCode
mv ../instance/etc-initd-script.tmp ../instance/etc-initd-script
verifyExitCode
cp ../instance/etc-initd-script $STARTUP_SCRIPT_DIRECTORY/apacheds-$APACHEDS_VERSION-$DEFAULT_INSTANCE_NAME
verifyExitCode

# Setting the correct permissions on executable files
chmod +x $STARTUP_SCRIPT_DIRECTORY/apacheds-$APACHEDS_VERSION-$DEFAULT_INSTANCE_NAME
verifyExitCode
chmod +x $APACHEDS_HOME_DIRECTORY/bin/apacheds
verifyExitCode
chmod +x $APACHEDS_HOME_DIRECTORY/bin/wrapper
verifyExitCode

# Creating the apacheds user and group (only if needed)
USER=`eval "id -u -n $RUN_AS_USER"`

# If we don't have any group, use the user's group
if [ "X$RUN_AS_GROUP" = "X" ]
then
        RUN_AS_GROUP=$RUN_AS_USER
fi

# Check that the group exists
GROUP=`eval "if grep -q $RUN_AS_GROUP /etc/group; then echo "$RUN_AS_GROUP"; else echo ""; fi"`

# Create the group if it does not exist
if [ ! "X$RUN_AS_GROUP" = "XGROUP" ]
then
	/usr/sbin/groupadd $RUN_AS_GROUP >/dev/null 2>&1 || :
	verifyExitCode
fi

if [ ! "X$RUN_AS_USER" = "X$USER" ]
then
	/usr/sbin/useradd -g $RUN_AS_GROUP -d $APACHEDS_HOME_DIRECTORY $RUN_AS_USER >/dev/null 2>&1 || :
	verifyExitCode
fi

# Modifying owner
chown -R $RUN_AS_USER:$RUN_AS_USER $APACHEDS_HOME_DIRECTORY
chown -R $RUN_AS_USER:$RUN_AS_USER $INSTANCES_HOME_DIRECTORY
chown $RUN_AS_USER:$RUN_AS_USER $STARTUP_SCRIPT_DIRECTORY/apacheds-$APACHEDS_VERSION-$DEFAULT_INSTANCE_NAME
