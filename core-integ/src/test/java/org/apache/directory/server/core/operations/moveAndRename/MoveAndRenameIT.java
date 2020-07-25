/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.operations.moveAndRename;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the MoveAndRename operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "MoveAndRenameIT",
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
    })
@ApplyLdifs(
    {
        "dn: ou=people,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: people",
        "",
            "dn: ou=Apache,ou=people,ou=system",
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Apache",
            "",
                "dn: ou=committers,ou=Apache,ou=people,ou=system",
                "objectClass: top",
                "objectClass: organizationalUnit",
                "ou: committers",
                "",
                    "dn: cn=elecharny,ou=committers,ou=Apache,ou=people,ou=system",
                    "objectClass: top",
                    "objectClass: person",
                    "cn: elecharny",
                    "sn: Emmanuel Lecharny",
                    "",
                    "dn: cn=kayyagari,ou=committers,ou=Apache,ou=people,ou=system",
                    "objectClass: top",
                    "objectClass: person",
                    "cn: kayyagari",
                    "sn: Kiran Ayyagari",
                    "",
                "dn: ou=PMC,ou=Apache,ou=people,ou=system",
                "objectClass: top",
                "objectClass: organizationalUnit",
                "ou: PMC",
                "",
        "dn: dc=domain,ou=system",
        "objectClass: top",
        "objectClass: domain",
        "objectClass: extensibleObject",
        "dc: domain"
    }
)
public class MoveAndRenameIT extends AbstractLdapTestUnit
{
    /**
     * Test a simple MoveAndRename move operation
     * cn=elecharny,ou=committers,ou=Apache,ou=people,ou=system will be moved to 
     * cn=emmanuel,dc=domain,ou=system
     * 
     * The original entry can have a 'cn', because the ObjectClass is organizationalPerson,
     * and will accept a cn as a new RDN
     * 
     * The old cn will be kept
     */
    @Test
    public void testMoveAndRenameSimpleKeepOldRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        // Get the nbSubordinates before the move
        Entry oldCommon = connection.lookup( "ou=people,ou=system", "*", "+" );
        Entry oldSuperior = connection.lookup( "ou=committers,ou=Apache,ou=people,ou=system", "*", "+" );
        Entry newSuperior = connection.lookup( "dc=domain,ou=system", "*", "+" );

        // Check the old common
        Attribute nbSubordinate = oldCommon.get( "nbSubordinates" );
        Attribute nbChildren = oldCommon.get( "nbChildren" );

        assertEquals( "5", nbSubordinate.getString() );
        assertEquals( "1", nbChildren.getString() );

        // The old superior
        nbSubordinate = oldSuperior.get( "nbSubordinates" );
        nbChildren = oldSuperior.get( "nbChildren" );

        assertEquals( "2", nbSubordinate.getString() );
        assertEquals( "2", nbChildren.getString() );

        // The new superior
        nbSubordinate = newSuperior.get( "nbSubordinates" );
        nbChildren = newSuperior.get( "nbChildren" );

        assertEquals( "0", nbSubordinate.getString() );
        assertEquals( "0", nbChildren.getString() );

        // Move and rename now
        String oldDn = "cn=elecharny,ou=committers,ou=Apache,ou=people,ou=system";
        String newDn = "cn=emmanuel,dc=domain,ou=system";

        assertNull( connection.lookup( newDn ) );
        assertNotNull( connection.lookup( oldDn ) );

        connection.moveAndRename( oldDn, newDn, true );

        assertNull( connection.lookup( oldDn ) );

        Entry movedEntry = connection.lookup( newDn, "*", "+" );
        assertNotNull( movedEntry );
        assertTrue( movedEntry.contains( "cn", "emmanuel" ) );
        assertFalse( movedEntry.contains( "cn", "elecharny" ) );

        // Get the nbSubordinates before the move
        oldCommon = connection.lookup( "ou=people,ou=system", "*", "+" );
        oldSuperior = connection.lookup( "ou=committers,ou=Apache,ou=people,ou=system", "*", "+" );
        newSuperior = connection.lookup( "dc=domain,ou=system", "*", "+" );

        // Check the old common
        nbSubordinate = oldCommon.get( "nbSubordinates" );
        nbChildren = oldCommon.get( "nbChildren" );

        assertEquals( "4", nbSubordinate.getString() );
        assertEquals( "1", nbChildren.getString() );

        // The old superior
        nbSubordinate = oldSuperior.get( "nbSubordinates" );
        nbChildren = oldSuperior.get( "nbChildren" );

        assertEquals( "1", nbSubordinate.getString() );
        assertEquals( "1", nbChildren.getString() );

        // The new superior
        nbSubordinate = newSuperior.get( "nbSubordinates" );
        nbChildren = newSuperior.get( "nbChildren" );

        assertEquals( "1", nbSubordinate.getString() );
        assertEquals( "1", nbChildren.getString() );
        connection.close();
    }


    /**
     * Test a simple MoveAndRename move operation
     * cn=elecharny,ou=committers,ou=Apache,ou=people,ou=system will be moved to 
     * cn=emmanuel,dc=domain,ou=system
     * 
     * The original entry can have a 'cn', because the ObjectClass is organizationalPerson,
     * and will accept a cn as a new RDN
     * 
     * The old cn will be removed
     */
    @Test
    public void testMoveAndRenameSimpleDeleteOldRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        // Get the nbSubordinates before the move
        Entry oldCommon = connection.lookup( "ou=people,ou=system", "*", "+" );
        Entry oldSuperior = connection.lookup( "ou=committers,ou=Apache,ou=people,ou=system", "*", "+" );
        Entry newSuperior = connection.lookup( "dc=domain,ou=system", "*", "+" );

        // Check the old common
        Attribute nbSubordinate = oldCommon.get( "nbSubordinates" );
        Attribute nbChildren = oldCommon.get( "nbChildren" );

        assertEquals( "5", nbSubordinate.getString() );
        assertEquals( "1", nbChildren.getString() );

        // The old superior
        nbSubordinate = oldSuperior.get( "nbSubordinates" );
        nbChildren = oldSuperior.get( "nbChildren" );

        assertEquals( "2", nbSubordinate.getString() );
        assertEquals( "2", nbChildren.getString() );

        // The new superior
        nbSubordinate = newSuperior.get( "nbSubordinates" );
        nbChildren = newSuperior.get( "nbChildren" );

        assertEquals( "0", nbSubordinate.getString() );
        assertEquals( "0", nbChildren.getString() );

        // Move and rename now
        String oldDn = "cn=elecharny,ou=committers,ou=Apache,ou=people,ou=system";
        String newDn = "cn=emmanuel,dc=domain,ou=system";

        assertNull( connection.lookup( newDn ) );
        assertNotNull( connection.lookup( oldDn ) );

        connection.moveAndRename( oldDn, newDn, false );

        assertNull( connection.lookup( oldDn ) );

        Entry movedEntry = connection.lookup( newDn, "*", "+" );
        assertNotNull( movedEntry );
        assertTrue( movedEntry.contains( "cn", "emmanuel" ) );
        assertTrue( movedEntry.contains( "cn", "elecharny" ) );

        // Get the nbSubordinates before the move
        oldCommon = connection.lookup( "ou=people,ou=system", "*", "+" );
        oldSuperior = connection.lookup( "ou=committers,ou=Apache,ou=people,ou=system", "*", "+" );
        newSuperior = connection.lookup( "dc=domain,ou=system", "*", "+" );

        // Check the old common
        nbSubordinate = oldCommon.get( "nbSubordinates" );
        nbChildren = oldCommon.get( "nbChildren" );

        assertEquals( "4", nbSubordinate.getString() );
        assertEquals( "1", nbChildren.getString() );

        // The old superior
        nbSubordinate = oldSuperior.get( "nbSubordinates" );
        nbChildren = oldSuperior.get( "nbChildren" );

        assertEquals( "1", nbSubordinate.getString() );
        assertEquals( "1", nbChildren.getString() );

        // The new superior
        nbSubordinate = newSuperior.get( "nbSubordinates" );
        nbChildren = newSuperior.get( "nbChildren" );

        assertEquals( "1", nbSubordinate.getString() );
        assertEquals( "1", nbChildren.getString() );

        connection.close();
    }


    /**
     * Test a simple MoveAndRename move operation
     * ou=Apache,ou=system will be moved to cn=test,ou=users,ou=system
     * 
     * The original entry can have a 'cn', because the ObjectClass is organizationalPerson,
     * and will accept a cn as a new RDN
     */
    @Test
    @Ignore
    public void testMoveAndRenameRdnNotInObjectClass() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "ou=Apache,ou=system";
        String newDn = "uid=test,ou=people,ou=system";

        assertNull( connection.lookup( newDn ) );
        assertNotNull( connection.lookup( oldDn ) );

        connection.moveAndRename( oldDn, newDn );

        assertNull( connection.lookup( oldDn ) );

        Entry movedEntry = connection.lookup( newDn );
        assertNotNull( movedEntry );
        assertTrue( movedEntry.contains( "cn", "test" ) );
        assertTrue( movedEntry.contains( "cn", "jDoe" ) );
        assertTrue( movedEntry.contains( "ou", "Apache" ) );

        connection.close();
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
            connection.moveAndRename( frDnStr, deDnStr, true );
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
}
