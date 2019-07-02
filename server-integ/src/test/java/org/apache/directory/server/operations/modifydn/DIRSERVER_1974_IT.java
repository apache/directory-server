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
package org.apache.directory.server.operations.modifydn;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.apache.directory.ldap.client.template.RequestBuilder;
import org.apache.directory.server.annotations.CreateLdapConnectionPool;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.CreateLdapConnectionPoolRule;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This Test case demonstrates an issue with the rename operation as 
 * describe in 
 * <a href='https://issues.apache.org/jira/browse/DIRSERVER-1974'>DIRSERVER-1974</a>.
 * 
 * To run this test, remove the <code>@Ignore</code> annotation and run the test.
 * Be patient, this test will take a while, but there should be some log messages 
 * to show progress.  It may succeed, but most likely will fail at some point.  To
 * speed the test up, you could load an external instance with the data and run
 * against it instead.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS( name = "classDS",
        partitions = {
                @CreatePartition( 
                        //type = MavibotPartition.class,
                        name = "example",
                        suffix = "dc=example,dc=com",
                        contextEntry = @ContextEntry(
                                entryLdif =
                                "dn: dc=example,dc=com\n" +
                                        "objectClass: domain\n" +
                                        "objectClass: top\n" +
                                        "dc: example\n\n"
                        ),
                        indexes = {
                                @CreateIndex(
                                    attribute = "objectClass" 
                                ),
                                @CreateIndex( 
                                    attribute = "dc" 
                                ),
                                @CreateIndex( 
                                    attribute = "ou" 
                                ),
                                @CreateIndex( 
                                    attribute = "uid" 
                                )
                        }
                )
        } )
@CreateLdapServer(
    transports = {
            @CreateTransport( protocol = "LDAP" )
    } )
@CreateLdapConnectionPool(
        maxActive = 4,
        maxIdle = 2,
        minIdle = 1 )
@ApplyLdifFiles( {
    "dirserver_1974_it.ldif"
} )
public class DIRSERVER_1974_IT extends AbstractLdapTestUnit
{
    private static final Logger logger = LoggerFactory.getLogger( DIRSERVER_1974_IT.class );
    private static final String BASE = "dc=example,dc=com";

    private static final EntryMapper<Entry> DEFAULT_ENTRY_MAPPER = new EntryMapper<Entry>() {
        @Override
        public Entry map( Entry entry ) throws LdapException {
            return entry;
        }
    };

    @Test
    public void testRenameWithALotOfDummiesAndSomeCustomAttributesAPI() throws LdapException, CursorException, IOException 
    {
        LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
        connection.bind( "uid=admin, ou=system", "secret" );
        
        Dn peopleDn = new Dn( "ou=people," + BASE );
            
        // Add the root entry
        connection.add( 
            new DefaultEntry( peopleDn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: people" ) );

        // Add 1000 children
        int dummyCount = 1000;

        for ( int i = 0; i < dummyCount; i++ ) 
        {
            String dn = "uid=uid-" + i + "," + peopleDn;
            
            connection.add( new DefaultEntry( dn,
                    "objectClass: top",
                    "objectClass: person",
                    "objectClass: organizationalPerson",
                    "objectClass: inetOrgPerson",
                    "uid", "uid-" + i,
                    "cn", "cn-" + i,
                    "sn", "sn-" + i,
                    "description", i + " is a person." ) );
            
            if ( i % 50 == 0 ) 
            {
                logger.debug( "Added person {}", i );
            }
        }
        
        EntryCursor cursor;
        int count = 0;
        
        // Now test the rename
        for ( int i = 0; i < 100; i++ ) 
        {
            String oldDnString = "uid=myra-ellen-amos, " + peopleDn.getName();
            String newDnString = "uid=tory-amos, " + peopleDn.getName();

            // Add an entry
            connection.add( new DefaultEntry( oldDnString,
                "objectClass: top", 
                "objectClass: person", 
                "objectClass: organizationalPerson", 
                "objectClass: inetOrgPerson", 
                "objectClass: portalPerson",
                "uid: myra-ellen-amos",
                "cn: Myra Ellen Amo",
                "sn: Amos",
                "active", Boolean.TRUE.toString(),
                "affiliation: Unknown",
                "timeZone: America/New_York",
                "description: Myra Ellen Amos is a person." ) );
            
            // Check it has been added
            Entry result = connection.lookup( oldDnString );
            
            assertNotNull (result );
            
            // Search for it
            cursor = connection.search( peopleDn, "(sn=amos)", SearchScope.ONELEVEL );
            count = 0;
            
            while ( cursor.next() )
            {
                Entry amos = cursor.get();
                assertEquals( "myra-ellen-amos", amos.get( "uid" ).getString() );
                assertEquals( "uid=myra-ellen-amos", amos.getDn().getRdn().getName() );
                
                count++;
            }
            
            assertEquals( 1, count );

            cursor.close();
            
            // Rename it
            connection.rename( oldDnString, "uid=tory-amos" );

            // Search for the old and renalme entry
            assertNull( connection.lookup( oldDnString ) );
            result = connection.lookup( newDnString );
            assertNotNull( result );

            // Search for the new entry
            cursor = connection.search( peopleDn, "(sn=amos)", SearchScope.ONELEVEL );
            count = 0;
            
            while ( cursor.next() )
            {
                Entry amos = cursor.get();
                assertEquals( "tory-amos", amos.get( "uid" ).getString() );
                assertEquals( "uid=tory-amos", amos.getDn().getRdn().getName() );
                
                count++;
            }
            
            assertEquals( 1, count );

            cursor.close();
            
            // Finally delete the new entry
            connection.delete( newDnString );
        }
        
        connection.close();
    }

    
    @Test
    @Ignore
    public void testRenameWithALotOfDummiesAndSomeCustomAttributes() {
        CreateLdapConnectionPoolRule connectionPool = new CreateLdapConnectionPoolRule();
        LdapConnectionTemplate template = connectionPool.getLdapConnectionTemplate();
        AddResponse response = null;

        final String peopleOu = "people";
        final String peopleRdn = "ou=" + peopleOu;
        final String peopleDnString = peopleRdn + "," + BASE;
        final Dn peopleDn = template.newDn( peopleDnString );
        
        template.execute( 
                new ConnectionCallback<Void>() {
                    @Override
                    public Void doWithConnection( LdapConnection connection ) throws LdapException {
                        logger.debug( "Add {}", peopleDnString );
                        connection.add( new DefaultEntry( peopleDn,
                                "objectClass", "top",
                                "objectClass", "organizationalUnit",
                                "ou", peopleOu ) );

                        int dummyCount = 1000;
                        logger.debug( "Add {} dummy people", dummyCount );
                        for ( int i = 1; i < dummyCount; i++ ) 
                        {
                            String uid = "uid-" + i;
                            String dn = "uid=" + uid + "," + peopleDn;
                            connection.add( new DefaultEntry( dn,
                                    "objectClass: top",
                                    "objectClass: person",
                                    "objectClass: organizationalPerson",
                                    "objectClass: inetOrgPerson",
                                    "uid", uid,
                                    "cn", "cn-" + i,
                                    "sn", "sn-" + i,
                                    "description", i + " is a person." ) );
                            if ( i % 50 == 0 ) 
                            {
                                logger.debug( "Added person {}", i );
                            }
                        }
                        return null;
                    }
            
                } );

        for ( int i = 0; i < 100; i++ ) {
            logger.info( "round {}", i );
            final String oldUid = "myra-ellen-amos";
            final String oldCn = "Myra Ellen Amos";
            final String oldRdn = "uid=" + oldUid;
            final String oldDnString = oldRdn + ", " + peopleDnString;
            final Dn oldDn = template.newDn( oldDnString );

            final String newUid = "tory-amos";
            final String newRdn = "uid=" + newUid;
            final String newDnString = newRdn + "," + peopleDnString;
            final Dn newDn = template.newDn( newDnString );

            response = template.add( oldDn,
                    new RequestBuilder<AddRequest>() {
                        @Override
                        public void buildRequest( AddRequest request ) throws LdapException {
                            request.getEntry()
                                    .add( "objectClass", "top", "person", "organizationalPerson", "inetOrgPerson", "portalPerson" )
                                    .add( "uid", oldUid )
                                    .add( "cn", oldCn )
                                    .add( "sn", "Amos" )
                                    .add( "active", Boolean.TRUE.toString() )
                                    .add( "affiliation", "Unknown" )
                                    .add( "timeZone", "America/New_York" )
                                    .add( "description", oldCn + " is a person." );
                        }
                    } );
            assertEquals( response.getLdapResult().getDiagnosticMessage(), ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

            assertNotNull( template.lookup( oldDn, DEFAULT_ENTRY_MAPPER ) );

            Entry found = template.searchFirst( peopleDn, FilterBuilder.equal( "sn", "amos" ),
                    SearchScope.ONELEVEL, DEFAULT_ENTRY_MAPPER );
            
            assertNotNull( found );
            Rdn foundRdn = found.getDn().getRdn();
            assertEquals( "uid", foundRdn.getType() );
            assertEquals( oldUid, foundRdn.getValue() );

            template.execute(
                    new ConnectionCallback<Void>() {
                        @Override
                        public Void doWithConnection( LdapConnection connection ) throws LdapException {
                            connection.rename( oldDnString, newRdn );
                            return null;
                        }
                    } );

            assertNull( template.lookup( oldDn, DEFAULT_ENTRY_MAPPER ) );
            assertNotNull( template.lookup( newDn, DEFAULT_ENTRY_MAPPER ) );

            found = template.searchFirst( peopleDn, FilterBuilder.equal( "sn", "amos" ),
                    SearchScope.ONELEVEL, DEFAULT_ENTRY_MAPPER );
            foundRdn = found.getDn().getRdn();
            assertNotNull( found );
            foundRdn = found.getDn().getRdn();
            assertEquals( "uid", foundRdn.getType() );
            assertEquals( newUid, foundRdn.getValue() );
            


            template.delete( newDn );
        }
    }


    /**
     * Modify Rdn of an entry, delete its old rdn value and search before and
     * after rename.
     */
    @Test
    public void testModifyRdnWithLotsOfDummies() throws Exception
    {
        String base = "dc=example,dc=com";
        String people = "people";
        String ouPeople = "ou=" + people;
        String dnPeople = ouPeople + "," + base;

        try (LdapConnection connection = ServerIntegrationUtils.getAdminConnection( getLdapServer() );) 
        {
            connection.loadSchema();
            
            logger.debug( "Add {}", dnPeople );
            connection.add( new DefaultEntry( dnPeople,
                    "objectClass", "top",
                    "objectClass", "organizationalUnit",
                    "ou", people ) );

            int dummyCount = 1000;
            logger.debug( "Add {} dummy people", dummyCount );
            for ( int i = 1; i < dummyCount; i++ ) 
            {
                String uid = "uid-" + i;
                String dn = "uid=" + uid + "," + dnPeople;
                connection.add( new DefaultEntry( dn,
                        "objectClass: top",
                        "objectClass: person",
                        "objectClass: organizationalPerson",
                        "objectClass: inetOrgPerson",
                        "uid", uid,
                        "cn", "cn-" + i,
                        "sn", "sn-" + i,
                        "description", i + " is a person." ) );
                if ( i % 50 == 0 ) 
                {
                    logger.debug( "Added person {}", i );
                }
            }
    
            // Create a person, cn value is rdn
            String oldUid = "mary-ellen-amos";
            String oldCn = "Myra Ellen Amos";
            String oldRdn = "uid=" + oldUid;

            // Renamed...
            String newUid = "tory-amos";
            String newRdn = "uid=" + newUid;
            String newDn = newRdn + "," + base;
        
            String oldDn = oldRdn + ", " + base;
            int i = 0;
            
            try
            {
                for ( ; i < 100; i++ ) 
                {
                    rename( connection, base, oldDn, oldUid, oldCn, newRdn, newUid, newDn );
                }
            }
            catch ( LdapException le )
            {
                System.out.println( "Error at loop " + i );
                
                try
                {
                    rename( connection, base, oldDn, oldUid, oldCn, newRdn, newUid, newDn );
                }
                catch ( LdapException le2 )
                {
                    le.printStackTrace();
                }
            }
        }
    }

    
    private void rename( LdapConnection connection, String base, String oldDn, String oldUid, String oldCn, 
        String newRdn, String newUid, String newDn ) throws LdapException
    {
        connection.add( new DefaultEntry( oldDn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "uid", oldUid,
            "cn", oldCn,
            "sn: Amos",
            "description", oldCn + " is a person." ) );
        Entry tori = connection.lookup( oldDn );
        assertNotNull( tori );
        assertTrue( tori.contains( "uid", "mary-ellen-amos" ) );
    
        connection.rename( oldDn, newRdn, true );
        assertNull( connection.lookup( oldDn ) );
        tori = connection.lookup( newDn );
        assertNotNull( tori );
    
        // Check values of uid
        assertTrue( tori.contains( "uid", newUid ) );
        assertFalse( tori.contains( "uid", oldUid ) ); // old value is gone
        assertEquals( 1, tori.get( "uid" ).size() );
    
        // now try a search
        Entry found = null;
        for ( Entry result : connection.search( base, "(sn=amos)", SearchScope.ONELEVEL ) )
        {
            if ( found == null )
            {
                found = result;
            }
            else
            {
                fail( "Found too many results" );
            }
        }
        assertNotNull( found );
        Rdn foundRdn = found.getDn().getRdn();
        assertEquals( "uid", foundRdn.getType() );
        assertEquals( newUid, foundRdn.getValue() );
    
        // Remove entry (use new rdn)
        connection.delete( newDn );
    }
}
