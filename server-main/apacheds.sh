#!/bin/sh
if [ -e target/apacheds-server-main-1.0-RC1-app.jar ] ; then
  echo uber jar exists
else
  echo uber jar not found need to build it
  mvn clean assembly:assembly
fi

java -Dlog4j.configuration=file://$(pwd)/log4j.properties -jar target/apacheds-server-main-1.0-RC1-app.jar server.xml 
