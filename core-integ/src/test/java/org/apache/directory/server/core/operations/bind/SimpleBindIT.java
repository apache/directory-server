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
package org.apache.directory.server.core.operations.bind;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test the Simple BindRequest using the API.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "SimpleBindIT", allowAnonAccess = true)
public class SimpleBindIT extends AbstractLdapTestUnit
{
    /** The ldap connection */
    private LdapConnection connection;


    @BeforeEach
    public void setup() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );
    }


    @AfterEach
    public void shutdown() throws Exception
    {
        connection.close();
    }

    /**
     * A method to do a search
     *
    private NamingEnumeration<SearchResult> search( DirContext ctx, String baseDn, String filter, int scope )
        throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( scope );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*", "+" } );
        ctx.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        NamingEnumeration<SearchResult> list = ctx.search( baseDn, filter, controls );
        return list;
    }


    /**
     * try to connect using a known user/password and read an entry.
     */
    @Test
    public void testSimpleBindAPrincipalAPassword() throws LdapException, IOException
    {
        connection.bind( "uid=admin,ou=system", "secret" );

        Entry entry = connection.lookup( "uid=admin,ou=system" );
        
        assertNotNull( entry );
    }


    /**
     * try to connect using a known user but with a bad password: we should get a invalidCredentials error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindAPrincipalBadPassword() throws LdapException, IOException
    {
        Assertions.assertThrows( LdapAuthenticationException.class, () -> 
        {
            connection.bind( "uid=admin,ou=system", "badsecret" );
        } );
    }


    /**
     * try to connect using a user with an invalid Dn: we should get a invalidDNSyntax error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindBadPrincipalAPassword() throws LdapException, IOException
    {
        Assertions.assertThrows( LdapInvalidDnException.class, () -> 
        {
            connection.bind( "admin", "badsecret" );
        } );
    }


    /**
     * try to connect using a unknown user: we should get a invalidCredentials error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindUnknowPrincipalAPassword() throws LdapException, IOException
    {
        Assertions.assertThrows( LdapAuthenticationException.class, () -> 
        {
            connection.bind( (String)null, "secret" );
        } );
    }


    /**
     * covers the anonymous authentication : we should be able to read the rootDSE, but that's it
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindNoPrincipalNoPassword() throws LdapException, IOException
    {
        connection.bind( (String)null, null );

        // We should be anonymous here.
        // Check that we can read the rootDSE
        Entry rootDse = connection.lookup( "" );
        assertNotNull( rootDse );

        // Check that we cannot read another entry being anonymous
        Entry entry = connection.lookup( "uid=admin,ou=system" );
        assertNull( entry );
    }


    /**
     * covers the Unauthenticated case : we should get a UnwillingToPerform error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindAPrincipalNoPassword() throws LdapException, IOException
    {
        Assertions.assertThrows( LdapUnwillingToPerformException.class, () -> 
        {
            connection.bind( "uid=admin,ou=system", null );
        } );
    }


    /**
     * covers the Unauthenticated case : we should get a UnwillingToPerform error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindAPrincipalNullPassword() throws Exception
    {
        Assertions.assertThrows( LdapUnwillingToPerformException.class, () -> 
        {
            LdapConnection connection = IntegrationUtils.getConnectionAs( getService(), "uid=admin,ou=system", null );
            assertFalse( connection.isAuthenticated() );
    
            connection = IntegrationUtils.getConnectionAs( getService(), "uid=admin,ou=system", "secret" );
    
            connection.bind( "uid=admin,ou=system", null );
        } );
    }


    /**
     * not allowed by the server. We should get a invalidCredentials error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindNoPrincipalAPassword() throws LdapException, IOException
    {
        Assertions.assertThrows( LdapAuthenticationException.class, () -> 
        {
            connection.bind( "", "secret" );
        } );
    }


    /**
     * try to connect using a known user/password and read an entry.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindWithDoubleQuote() throws LdapException, IOException
    {
        connection.bind( "uid=\"admin\",ou=\"system\"", "secret" );
        
        Entry entry = connection.lookup( "uid=admin,ou=system" );
        assertNotNull( entry );
    }
}
