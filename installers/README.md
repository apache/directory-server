> Licensed to the Apache Software Foundation (ASF) under one
> or more contributor license agreements.  See the NOTICE file
> distributed with this work for additional information
> regarding copyright ownership.  The ASF licenses this file
> to you under the Apache License, Version 2.0 (the
> "License"); you may not use this file except in compliance
> with the License.  You may obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing,
> software distributed under the License is distributed on an
> "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
> KIND, either express or implied.  See the License for the
> specific language governing permissions and limitations
> under the License.

# Installers

We provide installers for various OSes (**Linux**, **Windows**, **macOSX**) and architectures (32 bits and 64 bits). We also provide
generic binaries which don't install a daemon.

This document explains how those installers are working, for people who want to get deep into the process. As it, this
document is not targeting users, but rather developers.

## Supported OSes

We support:
* Linux (*rpm* and *deb* packages), for 32 bits and 64 bits
* Mac OSX (tm), for 32 bits and 64 bits
* Windows, 32 bit sonly
* a default binary that can be installed but does not provide a daemon


## Wrapper/daemon

It's convenient to have a package that can be installed as a daemon. We provide such package. We use 
a wrapper (Tanuki)

## Building installers

It's quite easy. You need to be in the _installers_ module and launch _mvn clean install -P<target>_ where _<target>_
is one of :
* **debian** : Debian installer, 32 and 64 bits
* **rpm** : Linux RH/CentOS installer
* **windows** : Windows 32 bits installer (we don't provide a 64 bits installer yet)
* **mac** : Mac OSX installer, 32 and 64 bits
* **bin** : Standard installer, 32 and 64 bits
* **archive** : archive - ie not an installer -.
* **docker** : A Docker installer
* **installers** : all the installers.

NOTE: In order to be able to build the *rpm*, *windows* and *deb* packages, you need to install the following tools:

### On Mac OSX:

* **deb**: *brew install dpkg*. **BEWARE**: it will install in */usr/local/bin*, instead of */usr/bin*.

You'll need to run the installers using this command line:

	mvn clean install -Pdebian -Dinstallers.dpkg=/usr/local/bin/dpkg

* **rpm**: *brew install rpm*. **BEWARE**: it will install in */usr/local/bin*, instead of */usr/bin*.

You'll need to run the installers using this command line:

	mvn clean install -Prpm -Dinstallers.dpkg=/usr/local/bin/rpmbuild


* **windows**: *brew install makensis*. **BEWARE**: it will install in */usr/local/bin*, instead of */usr/bin*.

You'll need to run the installers using this command line:

	mvn clean install -Pwindows -Dinstallers.makensis=/usr/local/bin/makensis

### 'bin' installer

Building a **bin** installer generates two files :
* _apacheds-2.0.0.AM27-SNAPSHOT-32bit.bin_
* _apacheds-2.0.0.AM27-SNAPSHOT-64bit.bin_

Those files are scripts concatenated with a *tar* file. The script will ask you to validate the
_Apache_ license, extract the tar file, launch the _install.sh_ script and cleanup temporary files once the 
installation is done. Here is the layout of the installer once executed, before the installation is ran:

	/
	|
	+--/instance
	|    |
	|    +-- config.ldif
	|    |
	|    +-- etc-initd-script
	|    |
	|    +-- log4j.properties
	|    |
	|    +-- wrapper-instance.conf
	|  
	+--/server
	|    |
	|    +--/bin
	|    |    |
	|    |    +-- apacheds*
	|    |    |
	|    |    +-- wrapper*
	|    |
	|    +--/conf
	|    |    |
	|    |    +-- wrapper.conf
	|    |
	|    +--/lib
	|    |    |
	|    |    +-- apacheds-service-2.0.0.AM27-SNAPSHOT.jar
	|    |    |
	|    |    +-- apacheds-wrapper-2.0.0.AM27-SNAPSHOT.jar
	|    |    |
	|    |    +-- libwrapper.so
	|    |    |
	|    |    +-- wrapper-3.2.3.jar
	|    |
	|    +-- NOTICE
	|    |
	|    +-- LICENSE
	|
	+--/sh
	     |
	     +-- functions.sh*
	     |
	     +-- install.sh*
	     |
	     +-- variables.sh*

The way the _instal.sh_ script works is that it uses the _variable.sh_ file content to configure the server: 
each variable (**@<name>@**) in the _/server/bin/apacheds_ script will be substituted with the default value or
 the provided value. Here is an exalple of what happens when the script is executed:
 
 	$ ./install.sh 
	Where do you want to install ApacheDS? [Default: /opt/apacheds-]

	Where do you want to install ApacheDS instances? [Default: /var/lib/apacheds-]

	What name do you want for the default instance? [Default: default]

	Where do you want to install the startup script? [Default: /etc/init.d]

Here are the variables that can be configured :

* **APACHEDS_HOME_DIRECTORY**	/opt/apacheds-2.0.0.AM26
* **INSTANCES_HOME_DIRECTORY**	/var/lib/apacheds-2.0.0.AM26
* **STARTUP_SCRIPT_DIRECTORY**	/etc/init.d
* **DEFAULT_INSTANCE_NAME**		default
* **RUN_AS_USER**				apacheds
* **RUN_AS_GROUP**				apacheds

Once the installer has been run, the server files are located in various places :

	/opt/apacheds-2.0.0.AM27-SNAPSHOT
	      |
	      +-- /bin
	      |    |
	      |    +-- apacheds*
	      |    |
	      |    +-- wrapper*
	      |
	      +-- /conf
	      |     |
	      |     +-- wrapper.conf
	      |
	      +-- /lib
	            |
	            +-- apacheds-service-2.0.0.AM27-SNAPSHOT.jar
	            |
	            +-- apacheds-wrapper-2.0.0.AM27-SNAPSHOT.jar
	            |
	            +-- libwrapper.so
	            |
	            +-- wrapper-3.2.3.jar
	.
	.
	.
	/var/lib/apacheds-2.0.0.AM27-SNAPSHOT
	          |
	          +-- /default
	                |
	                +-- /conf
	                |      |
	                |      +-- config.ldif
	                |      |
	                |      +-- log4j.properties
	                |      |
	                |      +-- wrapper-instance.conf
	                |
	                +-- /log
	                |
	                +-- /partitions
	                |
	                +-- /run
	.
	.
	.
	/etc/rc.d/init.d
	           |
	           +-- apacheds-2.0.0.AM27-SNAPSHOT-default

In the process, a user and group will be created if they didn't exist beforhand.

**NOTE**: if one want to run the server with a port under **389**, there are some configuration to change, as this is a privileged port.


### The 'bin' installer plugin

This plugin just create the initial installer, copying various files in the right place before creating the tar file.

At the end, it calls the _createInstaller.sh_ script that will generate the final package.

## Debugging a Maven plugin under Eclipse

Debugging one of the installers maven plugins in Eclipse is quite easy:

* First set a breakpoint in the _execute()_ function of the plugin you are interested in debugging
* Create a new *Maven Build* debug configuration
* Set the base Directory to _${workspace_loc:/apacheds-installers-2.0.0.AM27-SNAPSHOT}_ (or whatever version you are debugging). This can be done using the _workspace_ button below the input box. 
* Set the goals to _clean install_
* Set the _profile_ to match the plugin you want to debug (**mac**, **bin**, **debian**, **rpm**, **windows**, **archive**, **docker**, or **installers**)
* You are good to go !

