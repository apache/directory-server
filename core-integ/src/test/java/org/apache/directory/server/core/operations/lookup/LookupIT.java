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
package org.apache.directory.server.core.operations.lookup;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the lookup operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "LookupIT")
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=test,ou=system",
        "objectClass: person",
        "objectClass: inetorgPerson",
        "cn: test",
        "sn: sn_test",
        })
public class LookupIT extends AbstractLdapTestUnit
{
    /** The ldap connection */
    private LdapConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );
    }


    @After
    public void shutdown() throws Exception
    {
        connection.close();
    }


    /**
     * Test a lookup( Dn, "*") operation
     */
    @Test
    public void testLookupStar() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system", "*" );
        assertNotNull( entry );

        // Check that we don't have any operational attributes :
        // We should have only 3 attributes : objectClass, cn and sn
        assertEquals( 3, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( Dn, "+") operation
     */
    @Test
    public void testLookupPlus() throws Exception
    {
        getService().setDenormalizeOpAttrsEnabled( true );
        Entry entry = connection.lookup( "cn=test,ou=system", "+" );
        assertNotNull( entry );
        
        // We should have 9 attributes
        assertEquals( 10, entry.size() );

        // Check that all the user attributes are absent
        assertNull( entry.get( "cn" ) );
        assertNull( entry.get( "sn" ) );
        assertNull( entry.get( "objectClass" ) );
        
        /*
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
        */

        // Check that we have all the operational attributes :
        // We should have 6 operational attributes : createTime, createUser, entryCSN, entryDn, entryParentId and entryUUID
        assertNotNull( entry.get( "createTimestamp" ).getString() );
        assertNotNull( entry.get( "creatorsName" ) );
        assertEquals( "uid=admin,ou=system", entry.get( "creatorsName" ).getString() );
        assertNotNull( entry.get( "entryCSN" ).getString() );
        assertNotNull( entry.get( "entryUUID" ).getString() );
        assertNotNull( entry.get( "entryParentId" ).getString() );
        assertNotNull( entry.get( "entryDn" ));
        assertNotNull( entry.get( "nbChildren" ));
        assertNotNull( entry.get( "nbSubordinates" ));
        assertNotNull( entry.get( "hasSubordinates" ));
        assertNotNull( entry.get( "structuralObjectClass" ));
        assertEquals( "cn=test,ou=system", entry.get( "entryDn" ).getString() );
    }


    /**
     * Test a lookup( Dn, []) operation
     */
    @Test
    public void testLookupEmptyAtrid() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system", ( String[] ) null );
        assertNotNull( entry );

        // We should have 3 attributes
        assertEquals( 3, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( Dn ) operation
     */
    @Test
    public void testLookup() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system" );

        assertNotNull( entry );

        // We should have 3 attributes
        assertEquals( 3, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( Dn ) operation on the subschema subentry
     */
    @Test
    public void testLookupSubSchemaSubEntryOpAttrs() throws Exception
    {
        Entry entry = connection.lookup( "cn=schema", "+" );

        assertNotNull( entry );

        // We should have 12 attributes
        assertEquals( 12, entry.size() );

        // Check that all the operational attributes are present
        assertTrue( entry.containsAttribute( 
            "attributeTypes",
            "comparators",
            "createTimeStamp", 
            "creatorsName", 
            "modifiersName", 
            "modifyTimeStamp", 
            "ldapSyntaxes",
            "matchingRules",
            "normalizers",
            "objectClasses",
            "syntaxCheckers",
            "subtreeSpecification"
            ) );
    }


    /**
     * Test a lookup( Dn ) operation on the subschema subentry
     */
    @Test
    public void testLookupSubSchemaSubEntryUserAttrs() throws Exception
    {
        Entry entry = connection.lookup( "cn=schema", "*" );

        assertNotNull( entry );

        // We should have 2 attributes
        assertEquals( 2, entry.size() );

        // Check that all the operational attributes are present
        assertTrue( entry.containsAttribute( 
            "cn",
            "objectClass"
            ) );
    }


    /**
     * Test a lookup( Dn ) operation with a list of attributes
     */
    @Test
    public void testLookupWithAttrs() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system", "name" );
        assertNotNull( entry );

        // We should have 2 attributes
        assertEquals( 2, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertFalse( entry.containsAttribute( "objectClass" ) );
    }


    /**
     * Test a lookup( Dn ) operation with no attributes
     */
    @Test
    public void testLookupWithNoAttrs() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system", "1.1" );
        assertNotNull( entry );

        // We should have 0 attributes
        assertEquals( 0, entry.size() );
    }
    
    
    @Test
    public void testLookupSubordinates() throws LdapException
    {
        Entry entry = connection.lookup( "cn=test,ou=system", "*", "+" );
        
        assertNotNull( entry );

        // We should have 13 attributes
        assertEquals( 13, entry.size() );
        assertTrue( entry.containsAttribute( "nbChildren", "nbSubordinates", "hasSubordinates", "structuralObjectClass" ) );
        assertEquals( 0L, Long.parseLong( entry.get( "nbChildren" ).getString() ) );
        assertEquals( 0L, Long.parseLong( entry.get( "nbSubordinates" ).getString() ) );
        assertEquals( "FALSE", entry.get( "hasSubordinates" ).getString() );

        // Now lookup for the "ou=system"
        entry = connection.lookup( "ou=system", "*", "+" );
        
        assertNotNull( entry );

        // We should have 12 attributes
        assertEquals( 13, entry.size() );
        assertTrue( entry.containsAttribute( "nbChildren", "nbSubordinates", "hasSubordinates", "structuralObjectClass" ) );

        // we will have 6 children :
        // - ou=configuration
        // - ou=consumer
        // - ou = groups
        // - ou=users
        // - ou=prefNodeNames
        // - uid=admin
        // and 10 subordinates, as we have 3 children under ou=configuration and one under ou=groups
        assertEquals( 6L, Long.parseLong( entry.get( "nbChildren" ).getString() ) );
        assertEquals( 10L, Long.parseLong( entry.get( "nbSubordinates" ).getString() ) );
        assertEquals( "TRUE", entry.get( "hasSubordinates" ).getString() );
        
        // Check with only one of the two attributes 
        entry = connection.lookup( "ou=system", "nbChildren" );
        
        assertNotNull( entry );

        // We should have 1 attributes
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( "nbChildren" ) );
        assertFalse( entry.containsAttribute( "nbSubordinates" ) );

        // we will have 6 children :
        // - ou=configuration
        // - ou=consumer
        // - ou = groups
        // - ou=users
        // - ou=prefNodeNames
        // - uid=admin
        assertEquals( 6L, Long.parseLong( entry.get( "nbChildren" ).getString() ) );
        
        // And teh subordinates
        entry = connection.lookup( "ou=system", "nbSubordinates" );
        
        assertNotNull( entry );

        // We should have 1 attributes
        assertEquals( 1, entry.size() );
        assertFalse( entry.containsAttribute( "nbChildren" ) );
        assertTrue( entry.containsAttribute( "nbSubordinates" ) );

        // we will have 10 subordinates
        assertEquals( 10L, Long.parseLong( entry.get( "nbSubordinates" ).getString() ) );
    }
    
    
    @Test
    public void testLookupStructuralObjectClass() throws LdapException
    {
        Entry entry = connection.lookup( "cn=test,ou=system", "*", "+" );
        
        assertNotNull( entry );

        // We should have 12 attributes
        assertEquals( 13, entry.size() );
        assertTrue( entry.containsAttribute( "nbChildren", "nbSubordinates", "hasSubordinates", "structuralObjectClass" ) );
        assertEquals( 0L, Long.parseLong( entry.get( "nbChildren" ).getString() ) );
        assertEquals( 0L, Long.parseLong( entry.get( "nbSubordinates" ).getString() ) );
        assertEquals( "FALSE", entry.get( "hasSubordinates" ).getString() );
        assertEquals( "inetOrgPerson", entry.get( "structuralObjectClass" ).getString() );

        // Now lookup for the "ou=system"
        entry = connection.lookup( "ou=system", "*", "+" );
        
        assertNotNull( entry );

        // We should have 13 attributes
        assertEquals( 13, entry.size() );
        assertTrue( entry.containsAttribute( "nbChildren", "nbSubordinates", "hasSubordinates", "structuralObjectClass" ) );

        // we will have 6 children :
        // - ou=configuration
        // - ou=consumer
        // - ou = groups
        // - ou=users
        // - ou=prefNodeNames
        // - uid=admin
        // and 10 subordinates, as we have 3 children under ou=configuration and one under ou=groups
        assertEquals( 6L, Long.parseLong( entry.get( "nbChildren" ).getString() ) );
        assertEquals( 10L, Long.parseLong( entry.get( "nbSubordinates" ).getString() ) );
        assertEquals( "TRUE", entry.get( "hasSubordinates" ).getString() );
        assertEquals( "organizationalUnit", entry.get( "structuralObjectClass" ).getString() );
        
        // Check with only one of the two attributes 
        entry = connection.lookup( "ou=system", "structuralObjectClass" );
        
        assertNotNull( entry );

        // We should have 1 attributes
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( "structuralObjectClass" ) );
        assertEquals( "organizationalUnit", entry.get( "structuralObjectClass" ).getString() );
    }
}
