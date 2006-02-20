#!/bin/sh
# Really simple script to launch apacheds tools with RPM installer

APACHEDS_HOME=/usr/local/${app}-${app.version}
$JAVA_HOME/bin/java -jar $APACHEDS_HOME/bin/apacheds-tools.jar $@
