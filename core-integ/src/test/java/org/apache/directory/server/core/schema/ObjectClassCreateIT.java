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
package org.apache.directory.server.core.schema;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.api.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.util.JndiUtils;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "ObjectClassCreateIT")
public class ObjectClassCreateIT extends AbstractLdapTestUnit
{
    private String testOID = "1.3.6.1.4.1.18060.0.4.0.3.1.555555.5555.5555555";


    private void injectSchema() throws Exception
    {
        //--------------------------------------------------------------------
        // The accountStatus AT
        //--------------------------------------------------------------------
        Attributes attributes = new BasicAttributes( true );
        Attribute objectClassAttribute = new BasicAttribute( "objectClass" );

        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "metaTop" );
        objectClassAttribute.add( "metaAttributeType" );

        attributes.put( objectClassAttribute );

        attributes.put( "m-oid", "2.16.840.1.113730.3.2.22.249" );

        // The name
        attributes.put( "m-name", "accountStatus" );

        // The Obsolete flag
        attributes.put( "m-obsolete", "FALSE" );

        // The single value flag
        attributes.put( "m-singleValue", "TRUE" );

        // The collective flag
        attributes.put( "m-collective", "FALSE" );

        // The noUserModification flag
        attributes.put( "m-noUserModification", "FALSE" );

        // The usage
        attributes.put( "m-usage", "USER_APPLICATIONS" );

        // The equality matching rule
        attributes.put( "m-equality", "caseIgnoreMatch" );

        // The substr matching rule
        attributes.put( "m-substr", "caseIgnoreSubstringsMatch" );

        // The syntax
        attributes.put( "m-syntax", "1.3.6.1.4.1.1466.115.121.1.15" );

        // The superior
        attributes.put( "m-supAttributeType", "name" );

        // The description
        attributes.put( "m-description", "Account Status" );

        // Inject the AT
        Dn dn = new Dn( "ou=attributeTypes,cn=apachemeta" );
        dn = dn.add( MetaSchemaConstants.M_OID_AT + "=2.16.840.1.113730.3.2.22.249" );

        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attributes );

        //--------------------------------------------------------------------
        // The extendPerson OC
        //--------------------------------------------------------------------
        attributes = new BasicAttributes( true );
        objectClassAttribute = new BasicAttribute( "objectClass" );

        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "metaTop" );
        objectClassAttribute.add( "metaObjectClass" );

        attributes.put( objectClassAttribute );

        attributes.put( "m-oid", "2.16.840.1.113730.3.2.22" );

        // The name
        attributes.put( "m-name", "extendPerson" );

        // The Obsolete flag
        attributes.put( "m-obsolete", "FALSE" );

        // The Type list
        attributes.put( "m-typeObjectClass", "STRUCTURAL" );

        // The superiors
        attributes.put( "m-supObjectClass", "inetOrgPerson" );

        // The description
        attributes.put( "m-description", "Extended InetOrgPerson" );

        // The MAY list
        attributes.put( "m-may", "accountStatus" );

        // Inject the OC
        dn = new Dn( "ou=objectClasses,cn=apachemeta" );
        dn = dn.add( MetaSchemaConstants.M_OID_AT + "=2.16.840.1.113730.3.2.22" );

        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attributes );
    }


    /**
     * Gets relative Dn to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn of the objectClass container
     * @throws NamingException on error
     */
    private Dn getObjectClassContainer( String schemaName ) throws LdapInvalidDnException
    {
        return new Dn( "ou=objectClasses,cn=" + schemaName );
    }


    /*
     * Test that I cannot create an ObjectClass entry with an invalid name
     */
    @Test
    public void testCannotCreateObjectClassWithInvalidNameAttribute() throws Exception
    {
        Attributes attributes = new BasicAttributes( true );
        Attribute objectClassAttribute = new BasicAttribute( "objectClass" );

        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "metaTop" );
        objectClassAttribute.add( "metaObjectClass" );

        attributes.put( objectClassAttribute );

        attributes.put( "m-oid", "testOID" );

        // This name is invalid
        attributes.put( "m-name", "http://example.com/users/accounts/L0" );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( MetaSchemaConstants.M_OID_AT + "=" + testOID );

        try
        {
            getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attributes );
            fail(); // Should not reach this point
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /*
     * Test that I canotn create an ObjectClass entry with an invalid name
     */
    @Test
    public void testCannotCreateObjectClassWithNoObjectClass() throws Exception
    {
        Attributes attributes = new BasicAttributes( true );
        Attribute objectClassAttribute = new BasicAttribute( "objectClass" );

        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "metaTop" );
        objectClassAttribute.add( "metaObjectClass" );

        // Don't put the objectclasses in the entry : this is on purpose !
        // attributes.put( objectClassAttribute );

        attributes.put( "m-oid", "testOID" );

        // This name is invalid
        attributes.put( "m-name", "no-objectClasses" );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( MetaSchemaConstants.M_OID_AT + "=" + testOID );

        try
        {
            getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attributes );
            fail(); // Should not reach this point
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Test that if we create an OC with a superior OC then the AT are correctly
     * inherited.
     */
    @Test
    public void testCreateOCWithSuperior() throws Exception
    {
        injectSchema();

        // Now, check that we can add entries with this new OC
        Attributes entry = new BasicAttributes( true );
        Attribute objectClassAttribute = new BasicAttribute( "objectClass" );

        // The ObjectClass
        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "extendPerson" );

        entry.put( objectClassAttribute );

        // Mandatory attributes -- required in MUST list
        entry.put( "uid", "test" );
        entry.put( "sn", "test" );
        entry.put( "givenName", "test" );
        entry.put( "cn", "test" );
        entry.put( "displayName", "test-test" );
        entry.put( "initials", "tt" );
        entry.put( "accountStatus", "test" );

        // Create the context
        DirContext system = getSystemContext( getService() );

        try
        {
            system.createSubcontext( "cn=test", entry );
        }
        catch ( NamingException ne )
        {
            fail();
        }

    }


    /**
     * Test that if we create an AT with an ancestor, we can search and
     * get back the entry using its ancestor
     */
    @Test
    public void testCreateATWithSuperior() throws Exception
    {
        injectSchema();

        // Now, check that we can add entries with this new AT
        Attributes entry = new BasicAttributes( true );
        Attribute objectClassAttribute = new BasicAttribute( "objectClass" );

        // The ObjectClass
        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "extendPerson" );

        entry.put( objectClassAttribute );

        // Mandatory attributes -- required in MUST list
        entry.put( "uid", "test" );
        entry.put( "sn", "test" );
        entry.put( "givenName", "test" );
        entry.put( "cn", "test" );
        entry.put( "displayName", "test-test" );
        entry.put( "initials", "tt" );
        entry.put( "accountStatus", "accountStatusValue" );

        // Create the context
        DirContext system = getSystemContext( getService() );

        try
        {
            system.createSubcontext( "cn=test", entry );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls sc = new SearchControls();
        NamingEnumeration<SearchResult> result = system.search( "", "(name=accountStatusValue)", sc );

        boolean found = false;

        while ( result.hasMore() )
        {
            assertFalse( found );
            SearchResult searchResult = result.next();
            assertEquals( "accountStatusValue", searchResult.getAttributes().get( "accountStatus" ).get() );
            found = true;
        }

        found = false;
        result = system.search( "", "(accountStatus=accountStatusValue)", sc );

        while ( result.hasMore() )
        {
            assertFalse( found );
            SearchResult searchResult = result.next();
            assertEquals( "accountStatusValue", searchResult.getAttributes().get( "accountStatus" ).get() );
            found = true;
        }
    }
}
