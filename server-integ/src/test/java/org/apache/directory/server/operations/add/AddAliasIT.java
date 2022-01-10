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
package org.apache.directory.server.operations.add;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Integration tests for add operations on Alias.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    enableChangeLog = false,
    name = "DSAddAlias")
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class AddAliasIT extends AbstractLdapTestUnit
{
    private LdapConnection conn;


    @Test
    public void testAddAliasOnParent() throws Exception
    {
        try
        {
            conn = getAdminConnection( getLdapServer() );

            conn.add( new DefaultEntry(
                "cn=foo,ou=system",
                "objectClass: person",
                "objectClass: top",
                "cn: foo",
                "sn: Foo" ) );

            assertNotNull( conn.lookup( "cn=foo,ou=system" ) );

            conn.add( new DefaultEntry(
                "ou=alias,cn=foo,ou=system",
                "objectClass: top",
                "objectClass: extensibleObject",
                "objectClass: alias",
                "ou: alias",
                "aliasedObjectName: cn=foo,ou=system",
                "description: alias to father (branch)" ) );

            assertNotNull( conn.lookup( "ou=alias,cn=foo,ou=system" ) );
        }
        finally
        {
            // Cleanup entries now
            conn.delete( "ou=alias,cn=foo,ou=system" );
            conn.delete( "cn=foo,ou=system" );
        }
    }


    @Test
    public void testAddAliasWithSubordinate() throws Exception
    {
        try
        {
            conn = getAdminConnection( getLdapServer() );

            conn.add( new DefaultEntry(
                "cn=foo,ou=system",
                "objectClass: person",
                "objectClass: top",
                "cn: foo",
                "sn: Foo" ) );

            assertNotNull( conn.lookup( "cn=foo,ou=system" ) );

            conn.add( new DefaultEntry(
                "ou=alias,cn=foo,ou=system",
                "objectClass: top",
                "objectClass: extensibleObject",
                "objectClass: alias",
                "ou: alias",
                "aliasedObjectName: cn=foo,ou=system",
                "description: alias to father (branch)" ) );

            assertNotNull( conn.lookup( "ou=alias,cn=foo,ou=system" ) );

            try
            {
                conn.add( new DefaultEntry(
                    "ou=aliasChild,ou=alias,cn=foo,ou=system",
                    "objectClass: top",
                    "objectClass: extensibleObject",
                    "objectClass: alias",
                    "ou: aliasChild",
                    "aliasedObjectName: cn=foo,ou=system" ) );

                fail();
            }
            catch ( Exception e )
            {
                assertTrue( true );
            }

            assertNotNull( conn.lookup( "ou=alias,cn=foo,ou=system" ) );
            assertNull( conn.lookup( "ou=aliasChild,ou=alias,cn=foo,ou=system" ) );
        }
        finally
        {
            // Cleanup entries now
            conn.delete( "ou=alias,cn=foo,ou=system" );
            conn.delete( "cn=foo,ou=system" );
        }
    }


    /**
     * Add aliases with a cycle :
     * ou=system
     *   cn=foo
     *     cn=barAlias -> cn=bar
     *   cn=bar
     *     cn=fooAlias -> cn=foo
     * @throws Exception
     */
    @Test
    public void testAddAliasWithCycle() throws Exception
    {
        try
        {
            conn = getAdminConnection( getLdapServer() );

            conn.add( new DefaultEntry(
                "cn=test,ou=system",
                "objectClass: person",
                "objectClass: top",
                "cn: test",
                "sn: Test" ) );

            conn.add( new DefaultEntry(
                "cn=foo,cn=test,ou=system",
                "objectClass: person",
                "objectClass: top",
                "cn: foo",
                "sn: Foo" ) );

            conn.add( new DefaultEntry(
                "cn=bar,cn=test,ou=system",
                "objectClass: person",
                "objectClass: top",
                "cn: bar",
                "sn: Bar" ) );

            conn.add( new DefaultEntry(
                "cn=doh,cn=test,ou=system",
                "objectClass: person",
                "objectClass: top",
                "cn: doh",
                "sn: Doh" ) );

            //assertNotNull( conn.lookup( "cn=foo,cn=test,ou=system" ) );
            //assertNotNull( conn.lookup( "cn=bar,cn=test,ou=system" ) );

            conn.add( new DefaultEntry(
                "cn=barAlias,cn=foo,cn=test,ou=system",
                "objectClass: top",
                "objectClass: extensibleObject",
                "objectClass: alias",
                "cn: barAlias",
                "aliasedObjectName: cn=bar,cn=test,ou=system",
                "description: alias to father (branch)" ) );

            conn.add( new DefaultEntry(
                "cn=dohAlias,cn=bar,cn=test,ou=system",
                "objectClass: top",
                "objectClass: extensibleObject",
                "objectClass: alias",
                "cn: dohAlias",
                "aliasedObjectName: cn=doh,cn=test,ou=system",
                "description: alias to father (branch)" ) );

            //assertNotNull( conn.lookup( "cn=barAlias,cn=foo,cn=test,ou=system" ) );
            //assertNotNull( conn.lookup( "cn=fooAlias,cn=bar,cn=test,ou=system" ) );

            // Now, do a search
            EntryCursor cursor = conn.search( "cn=foo,cn=test,ou=system", "(objectClass=*)", SearchScope.SUBTREE, "*" );

            while ( cursor.next() )
            {
                //System.out.println( cursor.get().getDn() );
            }

            cursor.close();
        }
        finally
        {
            // Cleanup entries now
            conn.delete( "cn=barAlias,cn=foo,cn=test,ou=system" );
            conn.delete( "cn=dohAlias,cn=bar,cn=test,ou=system" );
            conn.delete( "cn=foo,cn=test,ou=system" );
            conn.delete( "cn=bar,cn=test,ou=system" );
            conn.delete( "cn=doh,cn=test,ou=system" );
            conn.delete( "cn=test,ou=system" );
        }
    }
}
