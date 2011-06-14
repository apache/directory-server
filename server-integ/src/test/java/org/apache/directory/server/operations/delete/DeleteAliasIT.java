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
package org.apache.directory.server.operations.delete;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getClientApiConnection;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Integration tests for delete operations on Alias.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS( 
    enableChangeLog = false,
    name = "DSDeleteAlias" )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class DeleteAliasIT extends AbstractLdapTestUnit
{
    private LdapConnection conn;

    
    @Before
    public void setup() throws Exception
    {
        conn = getClientApiConnection( getLdapServer() );
        
        if ( conn.lookup( "cn=foo,ou=system" ) == null )
        {
            conn.add( new DefaultEntry( 
                "cn=foo,ou=system", 
                "objectClass: person",
                "objectClass: top",
                "cn: foo",
                "sn: Foo" ) );
        }

        if ( conn.lookup( "ou=alias,ou=users,ou=system" ) == null )
        {
            conn.add( new DefaultEntry( 
                "ou=alias,ou=users,ou=system", 
                "objectClass: top",
                "objectClass: extensibleObject",
                "objectClass: alias",
                "ou: alias" ,
                "aliasedObjectName: cn=foo,ou=system",
                "description: alias to sibling (branch)" ) );
        }
    }

    /**
     * Tests normal delete operation of the alias and then the entry
     */
    @Test
    public void testDeleteAliasThenEntry() throws Exception
    {
        // Delete the alias
        assertNotNull( conn.lookup( "ou=alias,ou=users,ou=system" ) );
        conn.delete( "ou=alias,ou=users,ou=system" );
        assertNull( conn.lookup( "ou=alias,ou=users,ou=system" ) );

        // Now, delete the entry
        assertNotNull( conn.lookup( "cn=foo,ou=system" ) );
        conn.delete( "cn=foo,ou=system" );
        assertNull( conn.lookup( "cn=foo,ou=system" ) );
        
        conn.unBind();
        conn.close();
    }


    /**
     * Tests deletion of the entry then the alias
     */
    @Test
    public void testDeleteEntryThenAlias() throws Exception
    {
        conn = getClientApiConnection( getLdapServer() );

        // Delete the entry
        assertNotNull( conn.lookup( "cn=foo,ou=system" ) );
        conn.delete( "cn=foo,ou=system" );
        assertNull( conn.lookup( "cn=foo,ou=system" ) );
        
        // Now, delete the alias
        //assertNotNull( conn.lookup( "ou=alias,ou=users,ou=system" ) );
        conn.delete( "ou=alias,ou=users,ou=system" );
        assertNull( conn.lookup( "ou=alias,ou=users,ou=system" ) );

        conn.unBind();
        conn.close();
    }
}
