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
package org.apache.directory.server.core.operations.move;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * Test the move operation performances
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "MovePerfDS", 
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
    enableChangeLog = false)
public class MoveIT extends AbstractLdapTestUnit
{
    /**
     * Test a move operation :
     * cn=test,ou=system will be moved to cn=test,ou=users,ou=system
     */
    @Test
    public void testMove() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "cn=test,ou=system";
        String newDn = "cn=test,ou=users,ou=system";
        String newSuperior = "ou=users,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top", 
            "ObjectClass: person",
            "sn: TEST",
            "cn: test" );

        connection.add( entry );

        assertNull( connection.lookup( newDn ) );
        assertNotNull( connection.lookup( oldDn ) );

        connection.move( oldDn, newSuperior );

        assertNotNull( connection.lookup( newDn ) );
        assertNull( connection.lookup( oldDn ) );
        
        connection.close();
    }


    /**
     * Test a move operation :
     * cn=test,ou=system will be moved to cn=test,ou=users,ou=system
     */
    @Test
    public void testMoveWithChildren() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "cn=test,ou=system";
        String newDn = "cn=test,ou=users,ou=system";
        String newSuperior = "ou=users,ou=system";

        Entry test1 = new DefaultEntry( getService().getSchemaManager(), "cn=test1,ou=system",
            "ObjectClass: top", 
            "ObjectClass: person",
            "sn: Test1",
            "cn: tes1t" );

        Entry childTest1 = new DefaultEntry( getService().getSchemaManager(), "cn=childTest1,cn=test1,ou=system",
            "ObjectClass: top", 
            "ObjectClass: person",
            "sn: child test1",
            "cn: childTest1" );

        Entry test2 = new DefaultEntry( getService().getSchemaManager(), "cn=test2,ou=system",
            "ObjectClass: top", 
            "ObjectClass: person",
            "sn: Test2",
            "cn: test2" );

        Entry childTest2 = new DefaultEntry( getService().getSchemaManager(), "cn=childTest2,cn=test2,ou=system",
            "ObjectClass: top", 
            "ObjectClass: person",
            "sn: child test2",
            "cn: childTest2" );
        
        connection.add( test1 );
        connection.add( test2 );
        connection.add( childTest1 );
        connection.add( childTest2 );

        assertNotNull( connection.lookup( "cn=test1,ou=system" ) );
        assertNotNull( connection.lookup( "cn=test2,ou=system" ) );
        assertNotNull( connection.lookup( "cn=childTest1,cn=test1,ou=system" ) );
        assertNotNull( connection.lookup( "cn=childTest2,cn=test2,ou=system" ) );

        connection.move( "cn=test1,ou=system", "cn=test2,ou=system" );

        assertNull( connection.lookup( "cn=test1,ou=system" ) );
        assertNull( connection.lookup( "cn=childTest1,cn=test1,ou=system" ) );
        assertNotNull( connection.lookup( "cn=test2,ou=system" ) );
        assertNotNull( connection.lookup( "cn=childTest2,cn=test2,ou=system" ) );
        assertNotNull( connection.lookup( "cn=test1,cn=test2,ou=system" ) );
        assertNotNull( connection.lookup( "cn=childTest1,cn=test1,cn=test2,ou=system" ) );
        
        connection.close();
    }
}
