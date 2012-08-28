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
package org.apache.directory.server.operations.add;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;


/**
 * Test case to demonstrate DIRSERVER-631 ("Creation of entry with special (and
 * escaped) character in Rdn leads to wrong attribute value").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class AddingEntriesWithSpecialCharactersInRDNIT extends AbstractLdapTestUnit
{
    private Entry getPersonEntry( String sn, String cn ) throws LdapException
    {
        Entry entry = new DefaultEntry();
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "person" );
        entry.add( SchemaConstants.CN_AT, cn );
        entry.add( SchemaConstants.SN_AT, sn );

        return entry;
    }


    private Entry getOrgUnitEntry( String ou ) throws LdapException
    {
        Entry entry = new DefaultEntry();
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.add( SchemaConstants.OU_AT, ou );

        return entry;
    }


    /**
     * adding an entry with hash sign (#) in Rdn.
     * 
     * @throws Exception 
     */
    @Test
    public void testAddingWithHashRdn() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Entry personEntry = getPersonEntry( "Bush", "Kate#Bush" );
        String dn = "cn=Kate\\#Bush,ou=system";
        personEntry.setDn( new Dn( dn ) );
        connection.add( personEntry );

        EntryCursor cursor = connection.search( "ou=system", "(cn=Kate#Bush)", SearchScope.SUBTREE, "*" );

        boolean entryFound = false;

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            entryFound = true;

            assertTrue( personEntry.getDn().equals( entry.getDn() ) );
            Attribute cn = entry.get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate#Bush" ) );
        }

        assertTrue( "entry found", entryFound );

        cursor.close();
        connection.delete( dn );
        connection.close();
    }


    /**
     * adding an entry with comma sign (,) in Rdn.
     *    
     * @throws Exception 
     */
    @Test
    public void testAddingWithCommaInRdn() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Entry entry = getPersonEntry( "Bush", "Bush, Kate" );
        String dn = "cn=Bush\\, Kate,ou=system";
        entry.setDn( new Dn( dn ) );
        connection.add( entry );

        EntryCursor cursor = connection.search( "ou=system", "(cn=Bush, Kate)", SearchScope.SUBTREE, "*" );

        boolean entryFound = false;

        while ( cursor.next() )
        {
            Entry sr = cursor.get();
            entryFound = true;

            assertTrue( entry.getDn().equals( sr.getDn() ) );
            Attribute cn = sr.get( "cn" );
            assertNotNull( cn );

            assertTrue( cn.contains( "Bush, Kate" ) );
        }

        assertTrue( "entry found", entryFound );

        cursor.close();
        connection.delete( dn );
        connection.close();
    }


    /**
     * adding an entry with quotes (") in Rdn.
     */
    @Test
    public void testAddingWithQuotesInRdn() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Entry entry = getPersonEntry( "Messer", "Mackie \"The Knife\" Messer" );
        String dn = "cn=Mackie \\\"The Knife\\\" Messer,ou=system";
        entry.setDn( new Dn( dn ) );
        connection.add( entry );

        EntryCursor cursor = connection.search( "ou=system", "(cn=Mackie \"The Knife\" Messer)",
            SearchScope.SUBTREE, "*" );
        boolean entryFound = false;

        while ( cursor.next() )
        {
            Entry sr = cursor.get();
            entryFound = true;
            assertTrue( entry.getDn().equals( sr.getDn() ) );
            Attribute cn = sr.get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Mackie \"The Knife\" Messer" ) );
        }

        assertTrue( "entry found", entryFound );

        cursor.close();
        connection.delete( dn );
        connection.close();
    }


    /**
     * adding an entry with backslash (\) in Rdn.
     */
    @Test
    public void testAddingWithBackslashInRdn() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Entry entry = getOrgUnitEntry( "AC\\DC" );
        String dn = "ou=AC\\\\DC,ou=system";
        entry.setDn( new Dn( dn ) );
        connection.add( entry );

        EntryCursor cursor = connection.search( "ou=system", "(ou=AC\\5CDC)", SearchScope.SUBTREE, "*" );
        boolean entryFound = false;

        while ( cursor.next() )
        {
            Entry sr = cursor.get();
            entryFound = true;
            assertTrue( entry.getDn().equals( sr.getDn() ) );

            Attribute ou = sr.get( "ou" );
            assertNotNull( ou );
            assertTrue( ou.contains( "AC\\DC" ) );
        }

        assertTrue( "no entry found", entryFound );

        cursor.close();
        connection.delete( dn );
        connection.close();
    }


    /**
     * adding an entry with greater sign (>) in Rdn.
     * 
     * @throws Exception 
     */
    @Test
    public void testAddingWithGreaterSignInRdn() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Entry entry = getOrgUnitEntry( "East -> West" );
        String dn = "ou=East -\\> West,ou=system";
        entry.setDn( new Dn( dn ) );
        connection.add( entry );

        EntryCursor cursor = connection
            .search( "ou=system", "(ou=East -> West)", SearchScope.SUBTREE, "*" );

        boolean entryFound = false;

        while ( cursor.next() )
        {
            Entry sr = cursor.get();
            entryFound = true;

            assertTrue( entry.getDn().equals( sr.getDn() ) );
            Attribute ou = sr.get( "ou" );
            assertNotNull( ou );
            assertTrue( ou.contains( "East -> West" ) );
        }

        assertTrue( "entry found", entryFound );

        cursor.close();
        connection.delete( dn );
        connection.close();
    }


    /**
     * adding an entry with less sign (<) in Rdn.
     * 
     * @throws Exception 
     */
    @Test
    public void testAddingWithLessSignInRdn() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Entry entry = getOrgUnitEntry( "Scissors 8<" );
        String dn = "ou=Scissors 8\\<,ou=system";
        entry.setDn( new Dn( dn ) );
        connection.add( entry );

        EntryCursor cursor = connection.search( "ou=system", "(ou=Scissors 8<)", SearchScope.SUBTREE, "*" );

        boolean entryFound = false;

        while ( cursor.next() )
        {
            Entry sr = cursor.get();
            entryFound = true;

            assertTrue( entry.getDn().equals( sr.getDn() ) );

            Attribute ou = sr.get( "ou" );
            assertNotNull( ou );
            assertTrue( ou.contains( "Scissors 8<" ) );
        }

        assertTrue( "entry found", entryFound );

        cursor.close();
        connection.delete( dn );
        connection.close();
    }


    /**
     * adding an entry with semicolon (;) in Rdn.
     * 
     * @throws Exception 
     */
    @Test
    public void testAddingWithSemicolonInRdn() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Entry entry = getOrgUnitEntry( "semicolon group;" );
        String dn = "ou=semicolon group\\;,ou=system";
        entry.setDn( new Dn( dn ) );
        connection.add( entry );

        EntryCursor cursor = connection.search( "ou=system", "(ou=semicolon group;)", SearchScope.SUBTREE,
            "*" );

        boolean entryFound = false;

        while ( cursor.next() )
        {
            Entry sr = cursor.get();
            entryFound = true;

            assertTrue( entry.getDn().equals( sr.getDn() ) );
            Attribute ou = sr.get( "ou" );
            assertNotNull( ou );
            assertTrue( ou.contains( "semicolon group;" ) );
        }

        assertTrue( "entry found", entryFound );

        cursor.close();
        connection.delete( dn );
        connection.close();
    }


    /**
     * adding an entry with equals sign (=) in Rdn.
     * 
     * @throws Exception 
     */
    @Test
    public void testAddingWithEqualsInRdn() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Entry entry = getOrgUnitEntry( "nomen=omen" );
        String dn = "ou=nomen\\=omen,ou=system";
        entry.setDn( new Dn( dn ) );
        connection.add( entry );

        EntryCursor cursor = connection.search( "ou=system", "(ou=nomen=omen)", SearchScope.SUBTREE, "*" );

        boolean entryFound = false;

        while ( cursor.next() )
        {
            Entry sr = cursor.get();
            entryFound = true;

            assertTrue( entry.getDn().equals( sr.getDn() ) );
            Attribute ou = sr.get( "ou" );
            assertNotNull( ou );
            assertTrue( ou.contains( "nomen=omen" ) );
        }

        assertTrue( "entry found", entryFound );

        cursor.close();
        connection.delete( dn );
        connection.close();
    }


    @Test
    public void testAddRdnWithEscapedSpaces() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );
        connection.setTimeOut( -1 );

        Entry entry = new DefaultEntry(
            "cn=\\ User, ou=system",
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn:  User",
            "sn:  Name " );

        connection.add( entry );

        Entry addedEntry = connection.lookup( "cn=\\ User, ou=system" );

        assertNotNull( addedEntry );

        assertEquals( "Name", addedEntry.get( "sn" ).getString() );
        assertEquals( "User", addedEntry.get( "cn" ).getString() );
        assertEquals( 1, addedEntry.get( "cn" ).size() );
        assertTrue( addedEntry.contains( "cn", "User" ) );
        assertFalse( addedEntry.contains( "cn", " User" ) );
    }
}