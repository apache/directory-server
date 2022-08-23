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
package org.apache.directory.server.core.operations.modify;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests the modify() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "ModifyDelIT",
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=example,dc=com\n" +
                        "dc: example\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "sn"),
                        @CreateIndex(attribute = "cn"),
                        @CreateIndex(attribute = "c"),
                        @CreateIndex(attribute = "displayName")
                })
    })
@ApplyLdifs(
    {
        "dn: m-oid=2.2.0, ou=attributeTypes, cn=apachemeta, ou=schema",
        "objectclass: metaAttributeType",
        "objectclass: metaTop",
        "objectclass: top",
        "m-oid: 2.2.0",
        "m-name: integerAttribute",
        "m-description: the precursor for all integer attributes",
        "m-equality: integerMatch",
        "m-ordering: integerOrderingMatch",
        "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",
        "m-length: 0",
        "",
        "dn: ou=testing00,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing00",
        "integerAttribute: 0",
        "",
        "dn: ou=testing01,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing01",
        "integerAttribute: 1",
        "",
        "dn: ou=testing02,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing02",
        "integerAttribute: 2",
        "c: FR",
        "",
        "dn: ou=testing03,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing03",
        "integerAttribute: 3",
        "",
        "dn: ou=testing04,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing04",
        "integerAttribute: 4",
        "",
        "dn: ou=testing05,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing05",
        "integerAttribute: 5",
        "",
        "dn: ou=subtest,ou=testing01,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: subtest",
        "",
        "dn: cn=Heather Nova, dc=example,dc=com",
        "objectClass: top",
        "objectClass: person",
        "cn: Heather Nova",
        "sn: Nova",
        "telephoneNumber: 1 801 555 1212 ",
        "description: an American singer-songwriter",
        "",
        "dn: cn=Kim Wilde, dc=example,dc=com",
        "objectClass: top",
        "objectClass: person",
        "cn: Kim Wilde",
        "sn: Wilde",
        "telephoneNumber: 1 801 555 1212 ",
        "description: an American singer-songwriter",
        "description: She has blond hair",
        "",
        "dn: cn=with-dn, dc=example,dc=com",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetorgPerson",
        "cn: singer",
        "sn: manager",
        "telephoneNumber: 1 801 555 1212 ",
        "manager: cn=Heather Nova, dc=example,dc=com" })
public class ModifyDelIT extends AbstractLdapTestUnit
{
    private static final String DN_HEATHER_NOVA = "cn=Heather Nova, dc=example,dc=com";
    private static final String DN_KIM_WILDE = "cn=kim wilde, dc=example,dc=com";
    private static final String DN_TEST = "cn=test, dc=example,dc=com";
    private static final String DN_TESTING02 = "ou=testing02,dc=example,dc=com"; 


    /**
     * @throws Exception on errors
     */
    @BeforeAll
    protected static void createData() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( classDirectoryService ) )
        {
            /*
             * Check ou=testing00,dc=example,dc=com
             */
            Entry entry = conn.lookup( "ou=testing00,dc=example,dc=com" );
            assertNotNull( entry );
            assertNotEquals( 0, entry.size() );
            assertEquals( "testing00", entry.get( "ou" ).getString() );
            Attribute attribute = entry.get( "objectClass" );
            assertNotNull( attribute );
            assertTrue( attribute.contains( "top" ) );
            assertTrue( attribute.contains( "organizationalUnit" ) );
    
            /*
             * check ou=testing01,dc=example,dc=com
             */
            entry = conn.lookup( "ou=testing01,dc=example,dc=com" );
            assertNotNull( entry );
            assertNotEquals( 0, entry.size() );
            assertEquals( "testing01", entry.get( "ou" ).getString() );
            attribute = entry.get( "objectClass" );
            assertNotNull( attribute );
            assertTrue( attribute.contains( "top" ) );
            assertTrue( attribute.contains( "organizationalUnit" ) );
    
            /*
             * Check ou=testing02,dc=example,dc=com
             */
            entry = conn.lookup( "ou=testing02,dc=example,dc=com" );
            assertNotNull( entry );
            assertNotEquals( 0, entry.size() );
            assertEquals( "testing02", entry.get( "ou" ).getString() );
            attribute = entry.get( "objectClass" );
            assertNotNull( attribute );
            assertTrue( attribute.contains( "top" ) );
            assertTrue( attribute.contains( "organizationalUnit" ) );
    
            /*
             * Check ou=subtest,ou=testing01,dc=example,dc=com
             */
            entry = conn.lookup( "ou=subtest,ou=testing01,dc=example,dc=com" );
            assertNotNull( entry );
            assertNotEquals( 0, entry.size() );
            assertEquals( "subtest", entry.get( "ou" ).getString() );
            attribute = entry.get( "objectClass" );
            assertNotNull( attribute );
            assertTrue( attribute.contains( "top" ) );
            assertTrue( attribute.contains( "organizationalUnit" ) );
    
            /*
             *  Check entry cn=Heather Nova, dc=example,dc=com
             */
            entry = conn.lookup( DN_HEATHER_NOVA );
            assertNotNull( entry );
    
            // -------------------------------------------------------------------
            // Enable the nis schema
            // -------------------------------------------------------------------
    
            // check if nis is disabled
            String nisDn = "cn=nis," + SchemaConstants.OU_SCHEMA;
            entry = conn.lookup( nisDn );
            Attribute disabled = entry.get( "m-disabled" );
            boolean isNisDisabled = false;
    
            if ( disabled != null )
            {
                isNisDisabled = disabled.getString().equalsIgnoreCase( "TRUE" );
            }
    
            // if nis is disabled then enable it
            if ( isNisDisabled )
            {
                conn.modify( nisDn, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "m-disabled" ) );
            }

            entry = conn.lookup( nisDn );

            // -------------------------------------------------------------------
            // Add a bunch of nis groups
            // -------------------------------------------------------------------
            addNisPosixGroup( conn, "testGroup0", 0 );
            addNisPosixGroup( conn, "testGroup1", 1 );
            addNisPosixGroup( conn, "testGroup2", 2 );
            addNisPosixGroup( conn, "testGroup4", 4 );
            addNisPosixGroup( conn, "testGroup5", 5 );
    
            // Create a test account
            Entry testAccount = conn.lookup( DN_TEST );
            
            if ( testAccount == null )
            {
                conn.add( new DefaultEntry(
                    DN_TEST, 
                    "ObjectClass", "top",
                    "ObjectClass", "account",
                    "ObjectClass", "posixAccount",
                    "cn", "test",
                    "uid", "1",
                    "uidNumber", "1",
                    "gidNumber", "1",
                     "homeDirectory", "/",
                    "description", "A test account"
                    ) );
            }
        }
    }


    /**
     * Create a NIS group
     */
    private static void addNisPosixGroup( LdapConnection connection, String name, int gid ) throws Exception
    {
        String posixGroupDn = "cn=" + name + ",ou=groups, ou=system";
        Entry posixGroup = connection.lookup( posixGroupDn );
        
        if ( posixGroup == null )
        {
            connection.add( new DefaultEntry( posixGroupDn,
                "objectClass", "top",
                "objectClass", "posixGroup",
                "cn", name, 
                "gidNumber", Integer.toString( gid ) ) );
        }
    }


    //---------------------------------------------------------------------------------------------
    // Del operation
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.1 AT exists
    //   - The Value exists, it's not part of the Rdn, the AT is not singleValued
    //   - The Value exists, it's not part of the Rdn, the AT is singleValued, the AT is not in MUST
    //   - The Value exists, it's not part of the Rdn, the AT is singleValued, the AT is in MUST => error
    //   - The Value exists, it's part of the Rdn => error
    //   - The Value does not exists => error
    //   - Delete all the values, AT is not in MUST => AT must be removed
    //   - Delete all the values, AT is in MUST => error
    //---------------------------------------------------------------------------------------------
    /**
     * Delete a value from an existing AT. There are more than one value
     */
    @Test
    public void testModifyDelExistingEntryExistingATNotInRdnNotSV() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            createData();

        // A new description attribute value
        String deletedValue = "she has blond hair";

        conn.modify( DN_KIM_WILDE, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "description", deletedValue ) );

        // Verify that the attribute value has been removed
        Entry entry = conn.lookup( DN_KIM_WILDE );
        assertNotNull( entry.get( "description" ) );
        assertTrue( entry.contains( "description", "an American singer-songwriter" ) );
        assertFalse( entry.contains( "description", deletedValue ) );
        assertEquals( 1, entry.get( "description" ).size() );
        }
    }


    /**
     * Delete all the values from an existing AT not in MUST
     */
    @Test
    public void testModifyDelExistingEntryExistingATNotInRdnNotInMustNotSVAllValues() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            createData();

            // A new description attribute value
            conn.modify( DN_KIM_WILDE, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "description", "an American singer-songwriter", "she has blond hair" ) );
    
            // Verify that the attribute value has been removed
            assertNull( conn.lookup( DN_KIM_WILDE ).get( "description" ) );
        }
    }


    /**
     * Delete all the values from an existing AT in MUST
     */
    @Test
    public void testModifyDelExistingEntryExistingATNotInRdnNotSVAllValues() throws Exception
    {
        Assertions.assertThrows( LdapSchemaViolationException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                createData();
    
                conn.modify( DN_KIM_WILDE, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "sn", "Wilde" ) );
            }
        } );
    }


    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.2 AT does not exists.
    //---------------------------------------------------------------------------------------------
    /**
     * Remove a non existing AT from an entry, the AT is part of MAY/MUST
     */
    @Test
    public void testModifyDelExistingEntryNonExistingATInMay() throws Exception
    {
        Assertions.assertThrows( LdapNoSuchAttributeException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                createData();
    
                // A non existing AT 
                conn.modify( DN_HEATHER_NOVA, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "seeAlso", "cn=test" ) );
            }
        } );
    }


    /**
     * Remove a non existing AT from an entry, the AT is not part of MAY/MUST
     */
    @Test
    public void testModifyDelExistingEntryNonExistingATNotInMayMust() throws Exception
    {
        Assertions.assertThrows( LdapNoSuchAttributeException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                createData();
    
                // A non existing AT 
                conn.modify( DN_HEATHER_NOVA, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "c", "FR" ) );
            }
        } );
    }


    /**
     * Delete a value from an existing SingleValued AT, not in MUST, not in Rdn
     */
    @Test
    public void testModifyDelExistingEntryExistingATNotInRdnSV() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            createData();

            conn.modify( DN_TESTING02, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "c", "FR" ) );
            
            assertNull( conn.lookup( DN_TESTING02 ).get( "c" ) );
        }    
    }


    /**
     * Delete a value from an existing SingleValued AT, in MUST, not in Rdn
     */
    @Test
    public void testModifyDelExistingEntryExistingATNotInRdnSVInMust() throws Exception
    {
        Assertions.assertThrows( LdapSchemaViolationException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                createData();
                conn.modify( DN_TEST, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "uidNumber", "1" ) );
            }
        } );
    }


    /**
     * Delete a value part of the Rdn
     */
    @Test
    public void testModifyDelExistingEntryExistingATPartOfRdn() throws Exception
    {
        Assertions.assertThrows( LdapSchemaViolationException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                createData();
                conn.modify( DN_TEST, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "cn", "test" ) );
            }
        } );
    }


    /**
     * Delete an existing AT not part of the Rdn, not in MUST
     */
    @Test
    public void testModifyDelExistingEntryExistingATNoInRdnNotInMust() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            createData();
    
            conn.modify( DN_HEATHER_NOVA, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "description" ) );
    
            // Verify that the attribute has been removed
            Entry entry = conn.lookup( DN_HEATHER_NOVA );
            assertNull( entry.get( "description" ) );
        }
    }


    /**
     * Delete an existing AT not part of the Rdn, but in MUST
     */
    @Test
    public void testModifyDelExistingEntryExistingATNoInRdnInMust() throws Exception
    {
        Assertions.assertThrows( LdapSchemaViolationException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                createData();
        
                conn.modify( DN_HEATHER_NOVA, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "sn" ) );
            }
        } );
    }


    /**
     * Delete an existing AT part of the Rdn
     */
    @Test
    public void testModifyDelExistingEntryExistingATInRdn() throws Exception
    {
        Assertions.assertThrows( LdapSchemaViolationException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                conn.modify( DN_HEATHER_NOVA, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "cn" ) );
            }
        } );
    }


    /**
     * Delete a value not present in an existing AT
     */
    @Test
    public void testModifyDelExistingEntryValueNotPresentInExistingAT() throws Exception
    {
        Assertions.assertThrows( LdapNoSuchAttributeException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                createData();
        
                conn.modify( DN_HEATHER_NOVA, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "description", "Not present" ) );
            }
        } );
    }


    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.3 Entry is an alias
    //---------------------------------------------------------------------------------------------

    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.4 Entry is a referral.
    //---------------------------------------------------------------------------------------------

    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.2 Entry is a schema element.
    //---------------------------------------------------------------------------------------------

    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.2 The added AT is ObjectClass.
    //---------------------------------------------------------------------------------------------

    //---------------------------------------------------------------------------------------------
    // 2 Entry does not exist
    //---------------------------------------------------------------------------------------------
    /**
     * Del an AT in an entry which does not exist
     */
    @Test
    public void testModifyDelNotExistingEntry() throws Exception
    {
        Assertions.assertThrows( LdapNoSuchObjectException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                createData();
        
                conn.modify( "ou=absent,dc=example,dc=com", new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "cn", "test" ) );
            }
        } );
    }
}
