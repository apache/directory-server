#!/bin/bash

# One of the ugliest hacks you can find around!
#
# This script finds all subprojects by looking at pom.xmls starting from
# where it's invoked and generates a table of project names and descriptions.
# If there is no name element in project then it takes the artifactId.
# If there is no description element in the project then it prints "To be described...".
# This script ignores './pom.xml'.

xml_element_content=""

function get_xml_element # xml_file, xpath, xml_element
{
	xpath=$1
	xml_file=$2
	xml_element=$3

	xml_result=$(xml_grep $xpath $xml_file 2> /dev/null)
	if [ "$xml_result" != "" ]
	then
		xml_element_content=$(echo $xml_result | tr -d '\n' | sed "s/.*<${xml_element}>\(.*\)<\/${xml_element}>.*/\1/" | tr -s ' ')
	else
		xml_element_content=""
	fi
}


poms=$(for pom in $(find . -name "pom.xml"); do echo "${pom}"; done)

echo -e '<table>\n<tr><td>Project</td><td>Description</td></tr>'

for pom in $poms
do
	project_dir=$(echo $pom | sed 's/\.\/\(.*\)\/pom\.xml/\1/')
	get_xml_element '/project/name' $pom name
	project_name=$xml_element_content
	if [ "$project_name" == "" ]
	then
		get_xml_element '/project/artifactId' $pom artifactId
	        project_artifactId=$xml_element_content
		project_name=$project_artifactId
	fi
	get_xml_element '/project/description' $pom description
	project_description=$xml_element_content
	if [ "$project_description" == "" ]
        then
                project_description="To be described..."
        fi

	echo -e "<tr><td>${project_name}</td><td>$project_description</td></tr>"
done

echo '</table>'
