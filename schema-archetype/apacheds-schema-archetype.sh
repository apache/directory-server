#!/bin/sh

mvn archetype:create -DarchetypeGroupId=org.apache.directory.server -DarchetypeArtifactId=apacheds-schema-archetype -DarchetypeVersion=1.0.2-SNAPSHOT -DgroupId=$1 -DartifactId=$2

