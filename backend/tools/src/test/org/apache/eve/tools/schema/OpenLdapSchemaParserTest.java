/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.tools.schema;


import java.util.Map;

import junit.framework.TestCase;
import org.apache.ldap.common.schema.ObjectClassTypeEnum;


/**
 * Tests the OpenLDAP schema parser.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OpenLdapSchemaParserTest extends TestCase
{
    private OpenLdapSchemaParser parser;


    protected void setUp() throws Exception
    {
        super.setUp();

        parser = new OpenLdapSchemaParser();
        parser.setParserMonitor( new ParserMonitor()
        {
            public void matchedProduction( String prod )
            {
                System.out.println( prod );
            }
        });
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
        parser = null;
    }


    public void testSimpleAttributeTypeParse() throws Exception
    {
        String attributeTypeData = "# adding a comment  \n" +
            "attributetype ( 2.5.4.2 NAME 'knowledgeInformation'\n" +
            "        DESC 'RFC2256: knowledge information'\n" +
            "        EQUALITY caseIgnoreMatch\n" +
            "        SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} )";
        parser.parse( attributeTypeData );
        Map attributeTypes = parser.getAttributeTypes();
        AttributeTypeLiteral type = ( AttributeTypeLiteral ) attributeTypes.get( "2.5.4.2" );

        assertNotNull( type );
        assertEquals( "2.5.4.2", type.getOid() );
        assertEquals( "knowledgeInformation", type.getNames()[0] );
        assertEquals( "RFC2256: knowledge information", type.getDescription() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", type.getSyntax() );
        assertEquals( 32768, type.getLength() );
    }


    public void testComplexAttributeTypeParse() throws Exception
    {
        String attributeTypeData = "# adding a comment  \n" +
            "attributetype ( 2.5.4.2 NAME ( 'knowledgeInformation' 'asdf' ) \n" +
            "        DESC 'RFC2256: knowledge information'\n" +
            "        EQUALITY caseIgnoreMatch\n" +
            "        SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} )";
        parser.parse( attributeTypeData );
        Map attributeTypes = parser.getAttributeTypes();
        AttributeTypeLiteral type = ( AttributeTypeLiteral ) attributeTypes.get( "2.5.4.2" );

        assertNotNull( type );
        assertEquals( "2.5.4.2", type.getOid() );
        assertEquals( "knowledgeInformation", type.getNames()[0] );
        assertEquals( "RFC2256: knowledge information", type.getDescription() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", type.getSyntax() );
        assertEquals( 32768, type.getLength() );
    }


    public void testObjectClassParse() throws Exception
    {
        String objectClassData = "objectclass ( 2.5.6.6 NAME 'person'\n" +
            "        DESC 'RFC2256: a person'\n" +
            "        SUP top STRUCTURAL\n" +
            "        MUST ( sn $ cn )\n" +
            "        MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) )";
        parser.parse( objectClassData );
        Map objectClasses = parser.getObjectClassTypes();
        ObjectClassLiteral objectClass = ( ObjectClassLiteral ) objectClasses.get( "2.5.6.6" );

        assertNotNull( objectClass );
        assertEquals( "2.5.6.6", objectClass.getOid() );
        assertEquals( "person", objectClass.getNames()[0] );
        assertEquals( "RFC2256: a person", objectClass.getDescription() );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getClassType() );
        assertEquals( "sn", objectClass.getMust()[0] );
        assertEquals( "cn", objectClass.getMust()[1] );
        assertEquals( "userPassword", objectClass.getMay()[0] );
        assertEquals( "telephoneNumber", objectClass.getMay()[1] );
        assertEquals( "seeAlso", objectClass.getMay()[2] );
        assertEquals( "description", objectClass.getMay()[3] );
    }
}
