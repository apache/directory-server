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
package org.apache.directory.server.core.operations.rename;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test the rename operation performances
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "RenamePerfDS",
    partitions =
    {
        @CreatePartition(
            name = "example",
            suffix = "dc=example,dc=com",
            contextEntry =
                @ContextEntry(
                    entryLdif =
                        "dn: dc=example,dc=com\n" +
                        "dc: example\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
            indexes =
            {
                @CreateIndex(attribute = "objectClass"),
                @CreateIndex(attribute = "sn"),
                @CreateIndex(attribute = "cn")
            })
    },
    enableChangeLog = true)
public class RenameIT extends AbstractLdapTestUnit
{
    @Test
    public void testRenameUperCaseRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "cn=test1,ou=system";
        String newDn = "cn=test2,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: TEST",
            "cn: test1" );

        connection.add( entry );

        Entry original = connection.lookup( oldDn );

        assertNotNull( original );

        connection.rename( oldDn, "cn=TEST2" );

        Entry renamed = connection.lookup( newDn );

        assertNotNull( renamed );
        assertEquals( newDn, renamed.getDn().toString() );
        Attribute attribute = renamed.get( "cn" );

        assertTrue( attribute.contains( "test1" ) );
        assertTrue( attribute.contains( "test2" ) );
    }


    /**
     * Check that when doing a rename, with a MV RDN, and teh deleteOldRdn flag set to true,
     * we don't have the previous RDN in the entry.
     * 
     * @throws Exception
     */
    @Test
    public void testRenameMVAttributeDeleteOldRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "cn=test1,ou=system";
        String newDn = "cn=test2,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: TEST",
            "cn: test1" );

        connection.add( entry );

        Entry original = connection.lookup( oldDn );

        assertNotNull( original );

        connection.rename( oldDn, "cn=test2", true );

        assertNull( connection.lookup( oldDn ) );
        Entry renamed = connection.lookup( newDn );

        assertNotNull( renamed );
        assertEquals( newDn, renamed.getDn().toString() );
        Attribute attribute = renamed.get( "cn" );

        assertFalse( attribute.contains( "test1" ) );
        assertTrue( attribute.contains( "test2" ) );
    }


    /**
     * Check that when doing a rename, with a MV RDN, and the deleteOldRdn flag set to false,
     * we have the previous RDN in the entry.
     * 
     * @throws Exception
     */
    @Test
    public void testRenameMVAttributeKeepOldRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "cn=test1,ou=system";
        String newDn = "cn=test2,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: TEST",
            "cn: test1" );

        connection.add( entry );

        Entry original = connection.lookup( oldDn );

        assertNotNull( original );

        connection.rename( oldDn, "cn=test2", false );

        assertNull( connection.lookup( oldDn ) );
        Entry renamed = connection.lookup( newDn );

        assertNotNull( renamed );
        assertEquals( newDn, renamed.getDn().toString() );
        Attribute attribute = renamed.get( "cn" );

        assertTrue( attribute.contains( "test1" ) );
        assertTrue( attribute.contains( "test2" ) );
    }


    /**
     * Check that when doing a rename, with a SV RDN, we get an error if the deleteOldRdn flag is
     * set to false
     * 
     * @throws Exception
     */
    @Test
    public void testRenameSVAttributeKeepOldRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "c=FR,ou=system";
        String newDn = "c=DE,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: country",
            "c: FR" );

        connection.add( entry );

        Entry original = connection.lookup( oldDn );

        assertNotNull( original );

        try
        {
            connection.rename( oldDn, "c=DE", false );
        }
        catch ( Exception e )
        {
            System.out.println( e.getMessage() );
        }

        assertNotNull( connection.lookup( oldDn ) );
        assertNull( connection.lookup( newDn ) );
    }


    /**
     * Check that when doing a rename, with a SV RDN, we don't have the previous RDN in the entry,
     * if the deleteOldrdn flgg is set to true
     * 
     * @throws Exception
     */
    @Test
    public void testRenameSVAttributeDeleteOldRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "c=FR,ou=system";
        String newDn = "c=DE,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: country",
            "c: FR" );

        connection.add( entry );

        Entry original = connection.lookup( oldDn );

        assertNotNull( original );

        connection.rename( oldDn, "c=DE", true );

        assertNull( connection.lookup( oldDn ) );
        Entry renamed = connection.lookup( newDn );

        assertNotNull( renamed );
        assertEquals( newDn, renamed.getDn().toString() );
        Attribute countryName = renamed.get( "c" );

        assertTrue( countryName.contains( "DE" ) );
        assertFalse( countryName.contains( "FR" ) );
    }


    /**
     * Check that when doing a rename, with a SV RDN, we don't have the previous RDN in the entry,
     * if the deleteOldrdn flag is set to true
     * 
     * @throws Exception
     */
    @Test
    public void testRenameSVAttributeDeleteOldRdnExistingEntry() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String frDnStr = "c=FR,ou=system";
        String deDnStr = "c=DE,ou=system";

        // Create an entry that will collide with the rename
        Dn deDn = new Dn( deDnStr );
        Entry deEntry = new DefaultEntry( getService().getSchemaManager(), deDn,
            "ObjectClass: top",
            "ObjectClass: country",
            "c: DE" );

        connection.add( deEntry );

        // Create the entry that will be renamed
        Dn frDn = new Dn( frDnStr );
        Entry frEntry = new DefaultEntry( getService().getSchemaManager(), frDn,
            "ObjectClass: top",
            "ObjectClass: country",
            "c: FR" );

        connection.add( frEntry );

        Entry original = connection.lookup( frDn );

        assertNotNull( original );

        // rename the FR entry to DE entry : should fail as DE entry already exists
        try
        {
            connection.rename( frDnStr, "c=DE", true );
            fail();
        }
        catch ( LdapEntryAlreadyExistsException leaee )
        {
            Entry originalFr = connection.lookup( frDn );
            assertNotNull( originalFr );
            assertEquals( frDnStr, originalFr.getDn().toString() );
            assertTrue( originalFr.get( "c" ).contains( "FR" ) );
            assertFalse( originalFr.get( "c" ).contains( "DE" ) );

            Entry originalDe = connection.lookup( deDn );
            assertNotNull( originalDe );
            assertEquals( deDnStr, originalDe.getDn().toString() );
            assertTrue( originalDe.get( "c" ).contains( "DE" ) );
            assertFalse( originalDe.get( "c" ).contains( "FR" ) );
        }
    }


    /**
     * Check that when doing a rename, with a SV RDN, we get an error if the deleteOldRdn flag is
     * set to false
     * 
     * @throws Exception
     */
    @Test
    public void testRenameMvAttributeSVAttributeKeepOldRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "cn=test,ou=system";
        String newDn = "displayName=MyTest,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "ObjectClass: organizationalPerson",
            "ObjectClass: inetOrgPerson",
            "sn: TEST",
            "cn: test" );

        connection.add( entry );

        Entry original = connection.lookup( oldDn );

        assertNotNull( original );

        connection.rename( oldDn, "displayName=myTest", false );

        assertNull( connection.lookup( oldDn ) );
        Entry renamed = connection.lookup( newDn );

        assertNotNull( renamed );
        assertEquals( newDn, renamed.getDn().toString() );
        Attribute displayName = renamed.get( "displayName" );
        Attribute cn = renamed.get( "cn" );

        assertTrue( displayName.contains( "mytest" ) );
        assertTrue( cn.contains( "test" ) );
    }


    /**
     * Check that when doing a rename, from a MV attribute to a SV attribute, we don't have 
     * the previous RDN in the entry, if the deleteOldrdn flgg is set to true
     * 
     * @throws Exception
     */
    @Test
    public void testRenameMvAttributeSVAttributeDeleteOldRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "gn=test,ou=system";
        String newDn = "displayName=MyTest,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "ObjectClass: organizationalPerson",
            "ObjectClass: inetOrgPerson",
            "sn: TEST",
            "cn: test",
            "gn: test" );

        connection.add( entry );

        Entry original = connection.lookup( oldDn );

        assertNotNull( original );

        connection.rename( oldDn, "displayName=MyTest", true );

        assertNull( connection.lookup( oldDn ) );
        Entry renamed = connection.lookup( newDn );

        assertNotNull( renamed );
        assertEquals( newDn, renamed.getDn().toString() );
        Attribute displayName = renamed.get( "displayName" );
        Attribute cn = renamed.get( "gn" );

        assertTrue( displayName.contains( "mytest" ) );
        assertNull( cn );
    }
}
