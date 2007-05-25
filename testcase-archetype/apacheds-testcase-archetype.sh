#!/bin/sh

mvn archetype:create -DarchetypeGroupId=org.apache.directory.server -DarchetypeArtifactId=apacheds-testcase-archetype -DarchetypeVersion=1.0.2 -DgroupId=$1 -DartifactId=$2

