#!/bin/sh
# Really simple script for launching ApacheDS tools using IzPack

APACHEDS_HOME=%INSTALL_PATH
$JAVA_HOME/bin/java -jar $APACHEDS_HOME/bin/apacheds-tools.jar $@
