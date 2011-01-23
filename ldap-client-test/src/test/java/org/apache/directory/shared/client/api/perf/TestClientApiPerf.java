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
package org.apache.directory.shared.client.api.perf;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests for comparing performance of client API against various other LDAP client APIs
 *  (currently only compared against JNDI )
 * 
 * TODO print the performance results in a neat tabular fashion 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "ClientApiPerfTestDS", partitions =
    { @CreatePartition(name = "example", suffix = "dc=example,dc=com", contextEntry = @ContextEntry(entryLdif = "dn: dc=example,dc=com\n"
        + "dc: example\n" + "objectClass: top\n" + "objectClass: domain\n\n"), indexes =
        { @CreateIndex(attribute = "objectClass"), @CreateIndex(attribute = "dc"), @CreateIndex(attribute = "ou") }) })
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class TestClientApiPerf extends AbstractLdapTestUnit
{

    @Test
    public void testSearchPerformance() throws Exception
    {
        long t1 = System.currentTimeMillis();

        // Create connection
        LdapConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        connection.bind( "uid=admin,ou=system", "secret" );

        long t2 = System.currentTimeMillis();

        Cursor<Response> cursor = connection.search( "dc=example,dc=com", "(objectClass=*)", SearchScope.SUBTREE, "*" );
        while ( cursor.next() )
        {
            Response sr = cursor.get();
            SearchResultEntry sre = ( SearchResultEntry ) sr;
        }

        cursor.close();

        long t3 = System.currentTimeMillis();

        connection.close();

        long t4 = System.currentTimeMillis();

        System.out.println( "============== Client API =============" );
        System.out.println( "Time to create the connection: " + getElapsedTime( t1, t2 ) );
        System.out.println( "Time to perform the search: " + getElapsedTime( t2, t3 ) );
        System.out.println( "Time to close the connection: " + getElapsedTime( t3, t4 ) );
        System.out.println( "Total time: " + getElapsedTime( t1, t4 ) );
        System.out.println( "=======================================" );
    }


    @Test
    public void testSearchPerfWithJndi() throws NamingException
    {
        long t1 = System.currentTimeMillis();

        // Getting the connection
        DirContext ctx = jndiEnv( "localhost", ldapServer.getPort(), "", "uid=admin,ou=system", "secret", false );

        long t2 = System.currentTimeMillis();

        // Preparing the search controls
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        searchControls.setReturningAttributes( new String[]
            { "*" } );

        // Searching
        NamingEnumeration<SearchResult> ne = ctx.search( "dc=example,dc=com", "objectClass=*", searchControls );
        while ( ne.hasMoreElements() )
        {
            SearchResult searchResult = ( SearchResult ) ne.nextElement();
        }
        ne.close();

        long t3 = System.currentTimeMillis();

        // Closing the connection
        ctx.close();

        long t4 = System.currentTimeMillis();

        System.out.println( "================= JNDI ================" );
        System.out.println( "Time to create the connection: " + getElapsedTime( t1, t2 ) );
        System.out.println( "Time to perform the search: " + getElapsedTime( t2, t3 ) );
        System.out.println( "Time to close the connection: " + getElapsedTime( t3, t4 ) );
        System.out.println( "Total time: " + getElapsedTime( t1, t4 ) );
        System.out.println( "=======================================" );
    }


    /**
     * Creates the Jndi Context.
     * 
     * @param host
     *            the server host
     * @param port
     *            the server port
     * @param base
     *            the Active Directory Base
     * @param principal
     *            the Active Directory principal
     * @param credentials
     *            the Active Directory credentials
     * @return the jndi context
     * @throws NamingException
     */
    private DirContext jndiEnv( String host, int port, String base, String principal, String credentials, boolean ssl )
        throws NamingException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();

        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://" + host + ":" + port + "/" + base );
        env.put( Context.SECURITY_PRINCIPAL, principal );
        env.put( Context.SECURITY_CREDENTIALS, credentials );
        if ( ssl )
        {
            env.put( Context.SECURITY_PROTOCOL, "ssl" );
        }

        return new InitialDirContext( env );
    }


    private long getElapsedTime( long t1, long t2 )
    {
        return ( t2 - t1 );
    }
}
