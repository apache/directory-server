/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.tools.commands;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import junit.framework.TestCase;


/**
 * Abstract Class to extends to test the commands
 */
public class AbstractTestCase extends TestCase
{
    /** Flag to check if the server has been launched */
    protected boolean bindSuccessful;

    // General server settings
    protected final static String host = "localhost";
    protected final static int port = 10389;
    protected final static String user = "uid=admin,ou=system";
    protected final static String password = "secret";

    protected DirContext ctx;


    protected void setUp() throws Exception
    {
        super.setUp();

        bindSuccessful = false;

        // Set up the environment for creating the initial context
        Hashtable env = new Hashtable();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://" + host + ":" + port );

        // Authenticate as Admin
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, user );
        env.put( Context.SECURITY_CREDENTIALS, password );

        // Create the initial context
        try
        {
            ctx = new InitialDirContext( env );
        }
        catch ( NamingException ne )
        {
            bindSuccessful = false;
            return;
        }

        bindSuccessful = true;
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();

        if ( bindSuccessful )
        {
            try
            {
                deleteLDAPSubtree( "o=neworganization,dc=example,dc=com" );
                deleteLDAPSubtree( "ou=Product Development, dc=example, dc=com" );
                deleteLDAPSubtree( "ou=Product Testing, dc=example, dc=com" );
                deleteLDAPSubtree( "ou=Accounting, dc=example, dc=com " );
            }
            catch ( NamingException e )
            {

            }
        }
    }


    /**
     * Deletes recursively the LDAP subtree of the given DN
     * @param dn the root dn to delete from
     * @throws NamingException 
     * 
     */
    protected void deleteLDAPSubtree( String dn ) throws NamingException
    {
        boolean searchSuccessful = true;

        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );

        NamingEnumeration entries = null;
        try
        {
            entries = ctx.search( dn, "(objectClass=*)", ctls );
        }
        catch ( NamingException e )
        {
            searchSuccessful = false;
        }

        if ( searchSuccessful )
        {
            while ( entries.hasMoreElements() )
            {
                SearchResult sr = ( SearchResult ) entries.nextElement();
                deleteLDAPSubtree( sr.getNameInNamespace() );
            }
            ctx.destroySubcontext( dn );
        }
    }
}
