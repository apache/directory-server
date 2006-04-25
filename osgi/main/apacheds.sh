#!/bin/sh
if [ -e bin/felix.jar ] ; then
  echo Apache Directory OSGi Main exists
else
  echo Need to build Apache Directory OSGi Main ...
  mvn clean install
fi

java -Dlog4j.configuration=file://$(pwd)/log4j.properties -jar bin/felix.jar
