/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.schema.parser;


import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.parsers.OpenLdapSchemaParser;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OpenLdapObjectIdentifierMacro;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests the OpenLDAP schema parser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437016 $
 */
public class OpenLdapSchemaParserTest
{
    private OpenLdapSchemaParser parser;

    @Before
    public void setUp() throws Exception
    {
        parser = new OpenLdapSchemaParser();
        parser.setParserMonitor( new ConsoleParserMonitor() );
    }


    @After
    public void tearDown() throws Exception
    {
        parser = null;
    }


    @Test
    public void testSimpleAttributeTypeNoLength() throws Exception
    {
        String attributeTypeData = "attributetype ( 2.5.4.14 NAME 'searchGuide'\n"
            + "        DESC 'RFC2256: search guide, obsoleted by enhancedSearchGuide'\n"
            + "        SYNTAX 1.3.6.1.4.1.1466.115.121.1.25 )";

        parser.parse( attributeTypeData );
        List<AttributeType> attributeTypes = parser.getAttributeTypes();
        Map<String, AttributeType> mapAttributeTypes = mapAttributeTypes( attributeTypes );
        AttributeType attributeType = mapAttributeTypes.get( "2.5.4.14" );

        assertNotNull( attributeType );
        assertEquals( "2.5.4.14", attributeType.getOid() );
        assertEquals( "searchGuide", attributeType.getName() );
        assertEquals( "RFC2256: search guide, obsoleted by enhancedSearchGuide", attributeType.getDescription() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.25", attributeType.getSyntaxOid() );
    }


    private Map<String, AttributeType> mapAttributeTypes( List<AttributeType> attributeTypes )
    {
        Map<String, AttributeType> m = new HashMap<String, AttributeType>();

        for ( AttributeType attributeType : attributeTypes )
        {
            m.put( attributeType.getOid(), attributeType );
        }

        return m;
    }


    @Test
    public void testSimpleAttributeTypeParse() throws Exception
    {
        String attributeTypeData = "# adding a comment  \n" + "attributetype ( 2.5.4.2 NAME 'knowledgeInformation'\n"
            + "        DESC 'RFC2256: knowledge information'\n" + "        EQUALITY caseIgnoreMatch\n"
            + "        SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} )";
        parser.parse( attributeTypeData );
        List<AttributeType> attributeTypeList = parser.getAttributeTypes();
        Map<String, AttributeType> attributeTypes = mapAttributeTypes( attributeTypeList );
        AttributeType type = attributeTypes.get( "2.5.4.2" );

        assertNotNull( type );
        assertEquals( "2.5.4.2", type.getOid() );
        assertEquals( "knowledgeInformation", type.getName() );
        assertEquals( "RFC2256: knowledge information", type.getDescription() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", type.getSyntaxOid() );
        assertEquals( 32768, type.getSyntaxLength() );
    }


    @Test
    public void testAttributeTypeParseWithDescQuotes() throws Exception
    {
        String attributeTypeData = "# adding a comment  \n" + "attributetype ( 2.5.4.2 NAME 'knowledgeInformation'\n"
            + "        DESC 'RFC2256: \"knowledge\" information'\n" + "        EQUALITY caseIgnoreMatch\n"
            + "        SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} )";
        parser.parse( attributeTypeData );
        List<AttributeType> attributeTypeList = parser.getAttributeTypes();
        Map<String, AttributeType> attributeTypes = mapAttributeTypes( attributeTypeList );
        AttributeType type = attributeTypes.get( "2.5.4.2" );

        assertNotNull( type );
        assertEquals( "2.5.4.2", type.getOid() );
        assertEquals( "knowledgeInformation", type.getName() );
        assertEquals( "RFC2256: \"knowledge\" information", type.getDescription() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", type.getSyntaxOid() );
        assertEquals( 32768, type.getSyntaxLength() );
    }


    @Test
    public void testComplexAttributeTypeParse() throws Exception
    {
        String attributeTypeData = "# adding a comment  \n"
            + "attributetype ( 2.5.4.2 NAME ( 'knowledgeInformation' 'asdf' ) \n"
            + "        DESC 'RFC2256: knowledge information'\n" + "        EQUALITY caseIgnoreMatch\n"
            + "        SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} )";
        parser.parse( attributeTypeData );
        List<AttributeType> attributeTypeList = parser.getAttributeTypes();
        Map<String, AttributeType> attributeTypes = mapAttributeTypes( attributeTypeList );
        AttributeType type = attributeTypes.get( "2.5.4.2" );

        assertNotNull( type );
        assertEquals( "2.5.4.2", type.getOid() );
        assertEquals( "knowledgeInformation", type.getName() );
        assertEquals( "RFC2256: knowledge information", type.getDescription() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", type.getSyntaxOid() );
        assertEquals( 32768, type.getSyntaxLength() );
    }


    private Map<String, ObjectClass> mapObjectClasses( List<ObjectClass> objectClassList )
    {
        Map<String, ObjectClass> m = new HashMap<String, ObjectClass>();

        for ( ObjectClass objectClass : objectClassList )
        {
            m.put( objectClass.getOid(), objectClass );
        }

        return m;
    }


    @Test
    public void testObjectClassParse() throws Exception
    {
        String objectClassData = "objectclass ( 2.5.6.6 NAME 'person'\n" + "        DESC 'RFC2256: a person'\n"
            + "        SUP top STRUCTURAL\n" + "        MUST ( sn $ cn )\n"
            + "        MAY ( userPassword $ telephoneNumber $ seeAlso $ description ) )";
        parser.parse( objectClassData );
        List<ObjectClass> objectClassesList = parser.getObjectClassTypes();
        Map<String, ObjectClass> objectClasses = mapObjectClasses( objectClassesList );
        ObjectClass objectClass = objectClasses.get( "2.5.6.6" );

        assertNotNull( objectClass );
        assertEquals( "2.5.6.6", objectClass.getOid() );
        assertEquals( "person", objectClass.getName() );
        assertEquals( "RFC2256: a person", objectClass.getDescription() );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );
        assertEquals( "sn", objectClass.getMustAttributeTypeOids().get(0) );
        assertEquals( "cn", objectClass.getMustAttributeTypeOids().get(1) );
        assertEquals( "userPassword", objectClass.getMayAttributeTypeOids().get(0) );
        assertEquals( "telephoneNumber", objectClass.getMayAttributeTypeOids().get(1) );
        assertEquals( "seeAlso", objectClass.getMayAttributeTypeOids().get(2) );
        assertEquals( "description", objectClass.getMayAttributeTypeOids().get(3) );
    }


    @Test
    public void testObjectClassMultipleNames() throws Exception
    {
        String objectClassData = "objectclass ( 0.9.2342.19200300.100.4.4\n"
            + "\tNAME ( 'pilotPerson' 'newPilotPerson' )\n" + "\tSUP person STRUCTURAL\n"
            + "\tMAY ( userid $ textEncodedORAddress $ rfc822Mailbox $\n"
            + "\t\tfavouriteDrink $ roomNumber $ userClass $\n"
            + "\t\thomeTelephoneNumber $ homePostalAddress $ secretary $\n"
            + "\t\tpersonalTitle $ preferredDeliveryMethod $ businessCategory $\n"
            + "\t\tjanetMailbox $ otherMailbox $ mobileTelephoneNumber $\n"
            + "\t\tpagerTelephoneNumber $ organizationalStatus $\n"
            + "\t\tmailPreferenceOption $ personalSignature )\n" + "\t)";
        parser.parse( objectClassData );
        List<ObjectClass> objectClassesList = parser.getObjectClassTypes();
        Map<String, ObjectClass> objectClasses = mapObjectClasses( objectClassesList );
        ObjectClass objectClass = objectClasses.get( "0.9.2342.19200300.100.4.4" );

        assertNotNull( objectClass );
        assertEquals( "0.9.2342.19200300.100.4.4", objectClass.getOid() );
        assertEquals( "pilotPerson", objectClass.getName() );
        assertEquals( "newPilotPerson", objectClass.getNames().get(1) );
        assertEquals( ObjectClassTypeEnum.STRUCTURAL, objectClass.getType() );
        assertEquals( "person", objectClass.getSuperiorOids().get(0) );

        assertEquals( "userid", objectClass.getMayAttributeTypeOids().get(0) );
        assertEquals( "textEncodedORAddress", objectClass.getMayAttributeTypeOids().get(1) );
        assertEquals( "rfc822Mailbox", objectClass.getMayAttributeTypeOids().get(2) );
        assertEquals( "favouriteDrink", objectClass.getMayAttributeTypeOids().get(3) );
        assertEquals( "roomNumber", objectClass.getMayAttributeTypeOids().get(4) );
        assertEquals( "userClass", objectClass.getMayAttributeTypeOids().get(5) );
        assertEquals( "homeTelephoneNumber", objectClass.getMayAttributeTypeOids().get(6) );
        assertEquals( "homePostalAddress", objectClass.getMayAttributeTypeOids().get(7) );
        assertEquals( "secretary", objectClass.getMayAttributeTypeOids().get(8) );
        assertEquals( "personalTitle", objectClass.getMayAttributeTypeOids().get(9) );
        assertEquals( "preferredDeliveryMethod", objectClass.getMayAttributeTypeOids().get(10) );
        assertEquals( "businessCategory", objectClass.getMayAttributeTypeOids().get(11) );
        assertEquals( "janetMailbox", objectClass.getMayAttributeTypeOids().get(12) );
        assertEquals( "otherMailbox", objectClass.getMayAttributeTypeOids().get(13) );
        assertEquals( "mobileTelephoneNumber", objectClass.getMayAttributeTypeOids().get(14) );
        assertEquals( "pagerTelephoneNumber", objectClass.getMayAttributeTypeOids().get(15) );
        assertEquals( "organizationalStatus", objectClass.getMayAttributeTypeOids().get(16) );
        assertEquals( "mailPreferenceOption", objectClass.getMayAttributeTypeOids().get(17) );
        assertEquals( "personalSignature", objectClass.getMayAttributeTypeOids().get(18) );
    }


    @Test
    public void testParseOpenLdapCoreSchema() throws Exception
    {
        InputStream input = getClass().getResourceAsStream( "core.schema" );
        parser.parse( input );

        List<AttributeType> attributeTypes = parser.getAttributeTypes();
        List<ObjectClass> objectClassTypes = parser.getObjectClassTypes();
        Map<String, OpenLdapObjectIdentifierMacro> objectIdentifierMacros = parser.getObjectIdentifierMacros();

        assertEquals( 52, attributeTypes.size() );
        assertEquals( 27, objectClassTypes.size() );
        assertEquals( 0, objectIdentifierMacros.size() );
    }


    @Test
    public void testParseOpenLdapInetOrgPersonSchema() throws Exception
    {
        InputStream input = getClass().getResourceAsStream( "inetorgperson.schema" );
        parser.parse( input );

        List<AttributeType> attributeTypes = parser.getAttributeTypes();
        List<ObjectClass> objectClassTypes = parser.getObjectClassTypes();
        Map<String, OpenLdapObjectIdentifierMacro> objectIdentifierMacros = parser.getObjectIdentifierMacros();

        assertEquals( 9, attributeTypes.size() );
        assertEquals( 1, objectClassTypes.size() );
        assertEquals( 0, objectIdentifierMacros.size() );
    }


    @Test
    public void testParseOpenLdapCollectiveSchema() throws Exception
    {
        InputStream input = getClass().getResourceAsStream( "collective.schema" );
        parser.parse( input );

        List<AttributeType> attributeTypes = parser.getAttributeTypes();
        List<ObjectClass> objectClassTypes = parser.getObjectClassTypes();
        Map<String, OpenLdapObjectIdentifierMacro> objectIdentifierMacros = parser.getObjectIdentifierMacros();

        assertEquals( 13, attributeTypes.size() );
        assertEquals( 0, objectClassTypes.size() );
        assertEquals( 0, objectIdentifierMacros.size() );
        for ( AttributeType attributeTypeLiteral : attributeTypes )
        {
            assertTrue( attributeTypeLiteral.isCollective() );
        }
    }


    @Test
    public void testOpenLdapObjectIdentifiereMacros() throws Exception
    {
        InputStream input = getClass().getResourceAsStream( "dyngroup.schema" );
        parser.parse( input );

        List<AttributeType> attributeTypes = parser.getAttributeTypes();
        List<ObjectClass> objectClassTypes = parser.getObjectClassTypes();
        Map<String, OpenLdapObjectIdentifierMacro> objectIdentifierMacros = parser.getObjectIdentifierMacros();

        assertEquals( 2, attributeTypes.size() );
        assertEquals( 2, objectClassTypes.size() );
        assertEquals( 8, objectIdentifierMacros.size() );

        // check that all macros are resolved
        for ( OpenLdapObjectIdentifierMacro macro : objectIdentifierMacros.values() )
        {
            assertTrue( macro.isResolved() );
            assertNotNull( macro.getResolvedOid() );
            assertTrue( macro.getResolvedOid().matches( "[0-9]+(\\.[0-9]+)+" ) );
        }

        // check that OIDs in attribute types and object classes are resolved
        for ( ObjectClass objectClass : objectClassTypes )
        {
            List<String> asList = objectClass.getNames();
            if ( asList.contains( "groupOfURLs" ) )
            {
                assertEquals( "2.16.840.1.113730.3.2.33", objectClass.getOid() );
            }
            else if ( asList.contains( "dgIdentityAux" ) )
            {
                assertEquals( "1.3.6.1.4.1.4203.666.11.8.2.1", objectClass.getOid() );
            }
            else
            {
                fail( "object class 'groupOfURLs' or 'dgIdentityAux' expected" );
            }
        }
        
        for ( AttributeType attributeType : attributeTypes )
        {
            List<String> asList = attributeType.getNames();
            
            if ( asList.contains( "memberURL" ) )
            {
                assertEquals( "2.16.840.1.113730.3.1.198", attributeType.getOid() );
            }
            else if ( asList.contains( "dgIdentity" ) )
            {
                assertEquals( "1.3.6.1.4.1.4203.666.11.8.1.1", attributeType.getOid() );
            }
            else
            {
                fail( "attribute type 'memberURL' or 'dgIdentity' expected" );
            }
        }
    }

}
