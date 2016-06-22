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

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
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
        "dn: ou=Apache,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "ou: Apache",
        "cn: jDoe",
        "sn: John Doe",
        "",
        "dn: ou=people,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: people",
        "",
        "dn: dc=domain,ou=system",
        "objectClass: top",
        "objectClass: domain",
        "objectClass: extensibleObject",
        "dc: domain",
        "",
        "dn: gn=john+cn=doe,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetorgperson",
        "sn: John Doe",
        "gn: john",
        "cn: doe"
    }
)
public class MoveAndRenameIT extends AbstractLdapTestUnit
{
    /**
     * Test a simple MoveAndRename move operation
     * ou=Apache,ou=system will be moved to cn=test,ou=users,ou=system
     * 
     * The original entry can have a 'cn', because the ObjectClass is organizationalPerson,
     * and will accept a cn as a new RDN
     */
    @Test
    public void testMoveAndRenameSimple() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "ou=Apache,ou=system";
        String newDn = "cn=test,ou=people,ou=system";

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
     * Test a simple MoveAndRename move operation
     * ou=Apache,ou=system will be moved to cn=test,ou=users,ou=system
     * 
     * The original entry can have a 'cn', because the ObjectClass is organizationalPerson,
     * and will accept a cn as a new RDN
     */
    @Test
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
}
