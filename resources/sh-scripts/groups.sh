#!/bin/sh

RESOURCES=../../resources

for project in *
do
	cd $project &> /dev/null
	if [ $? -ne 0 ]
	then
		continue
	fi
	if [ ! -d "src/main/java" ]
	then
		cd - &> /dev/null
		continue
	fi
	ROOTPACKAGENAME=$(grep -h '^package org\.apache\.directory\.server.*;$' * -R | sed 's/\r//g' | sort -u | head -1 | sed 's/\(package \)//' | tr -d ';')
	if [ ! -f pom.xml ]
	then
		cd - &> /dev/null
		continue
	fi
	NAME=$($RESOURCES/pomutils/name.sh ./pom.xml)

	echo $ROOTPACKAGENAME
	echo $NAME
	echo "----------------------------------------------------------"
	cd - &> /dev/null
done
