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
package org.apache.ldap.server;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NoPermissionException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;


/**
 * A set of miscellanous tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MiscTest extends AbstractServerTest
{
    /**
     * Cleans up old database files on creation.
     */
    public MiscTest()
    {
    }


    /**
     * Customizes setup for each test case.
     *
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        if ( this.getName().equals( "testDisableAnonymousBinds" ) )
        {
            configuration.setAllowAnonymousAccess( false );
        }
        super.setUp();
    }


    /**
     * Test to make sure anonymous binds are disabled when going through
     * the wire protocol.
     *
     * @throws Exception if anything goes wrong
     */
    public void testDisableAnonymousBinds() throws Exception
    {
        // Use the SUN JNDI provider to hit server port and bind as anonymous

        final Hashtable env = new Hashtable();

        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port + "/ou=system" );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        try
        {
            new InitialContext( env );
            fail( "If anonymous binds are disabled we should never get here!" );
        }
        catch ( NoPermissionException e )
        {
        }
    }


    /**
     * Reproduces the problem with
     * <a href="http://issues.apache.org/jira/browse/DIREVE-239">DIREVE-239</a>.
     *
     * @throws Exception if anything goes wrong
     */
    public void testAdminAccessBug() throws Exception
    {
        // Use the SUN JNDI provider to hit server port and bind as anonymous

        final Hashtable env = new Hashtable();

        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );
        env.put("java.naming.ldap.version", "3");
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        Attributes attributes = new BasicAttributes();
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        attributes.put( objectClass );
        attributes.put( "ou", "blah" );
        InitialDirContext ctx = new InitialDirContext( env );
        ctx.createSubcontext( "ou=blah,ou=system", attributes );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[] { "+" } );
        NamingEnumeration list = ctx.search( "ou=blah,ou=system", "(objectClass=*)", controls );
        SearchResult result = ( SearchResult ) list.next();
        list.close();
        Attribute creatorsName = result.getAttributes().get( "creatorsName" );
        assertEquals( "", creatorsName.get() );
    }
}
