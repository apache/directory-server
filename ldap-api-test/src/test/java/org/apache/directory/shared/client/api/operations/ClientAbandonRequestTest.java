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

package org.apache.directory.shared.client.api.operations;


import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.SearchListener;
import org.apache.directory.shared.ldap.client.api.messages.SearchRequest;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultDone;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultEntry;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultReference;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A test class for ClientAbandonRequest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith(SiRunner.class)
@CleanupLevel(Level.CLASS)
public class ClientAbandonRequestTest
{
    /** The server instance */
    public static LdapServer ldapServer;

    private LdapConnection connection;

    private CoreSession session;

    private static final Logger LOG = LoggerFactory.getLogger( LdapConnection.class );


    @Before
    public void setup() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapServer.getPort() );
        LdapDN bindDn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( bindDn.getUpName(), "secret" );

        session = ldapServer.getDirectoryService().getSession();
    }


    @Test
    public void testAbandonSearch() throws Exception
    {
        // injecting some large number of values to keep the
        // followed search operation to run for a while
        
        // NOTE: currently the search for 1000 entries is very fast hence is not getting
        // aborted but the client ignores the returned search results
        // I tried with 10000 entries but only to encounter a OOM :(
        // TODO test this with higher number of entries to actually let the server abort
        // the operation
        int numEntries = 1000;
        for ( int i = 0; i < numEntries; i++ )
        {
            String s = String.valueOf( i );
            LdapDN dn = new LdapDN( "cn=" + s + ",ou=system" );
            Entry entry = new DefaultClientEntry( dn );
            entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC );
            entry.add( SchemaConstants.CN_AT, s );
            entry.add( SchemaConstants.SN_AT, s );

            connection.add( entry );
        }

        SearchRequest sr = new SearchRequest();
        sr.setFilter( "(cn=*)" );
        sr.setBaseDn( "ou=system" );
        sr.setScope( SearchScope.ONELEVEL );
        sr.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );

        final AtomicInteger count = new AtomicInteger( 0 );

        SearchListener sl = new SearchListener()
        {
            public void searchDone( LdapConnection connection, SearchResultDone searchResultDone ) throws LdapException
            {
            }


            public void referralFound( LdapConnection connection, SearchResultReference searchResultReference )
                throws LdapException
            {
            }


            public void entryFound( LdapConnection connection, SearchResultEntry searchResultEntry )
                throws LdapException
            {
                count.incrementAndGet();
                LOG.debug( "fetched entry count {}" + count );
            }
        };

        connection.search( sr, sl );

        // message ID will be 1(for bind) + numEntries(add ops) + 1 (abandon) 
        connection.abandon( numEntries + 2 );

        assertTrue( numEntries != count.get() );
    }

}
