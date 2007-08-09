#!/bin/sh
# Really simple script to launch apacheds tools with RPM installer

. /etc/sysconfig/apacheds

$JAVA_HOME/bin/java -jar $APACHEDS_HOME/bin/apacheds-tools.jar $@
