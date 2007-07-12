#!/bin/sh
if [ -e target/apacheds-server-main-1.5.1-SNAPSHOT-app.jar ] ; then
  echo uber jar exists
else
  echo uber jar not found need to build it
  mvn clean assembly:assembly
fi

java -Dlog4j.debug -Dlog4j.configuration=file:./log4j.properties -jar target/apacheds-server-main-1.5.1-SNAPSHOT-app.jar server.xml 
