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
package org.apache.directory.server;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;

/**
 * Simple test for DefaultAuthorizationService. Adds two users emtries below
 * ou=users,ou=system and checks whether a user is only allowed to see his own
 * entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $
 */
public class DefaultAuthorizationTest extends AbstractServerTest
{
    private LdapContext ctx = null;

    public static final String TORI_RDN = "cn=Tori Amos";

    public static final String FIONA_RDN = "cn=Fiona Apple";

    public static final String FIONA_DN = "cn=Fiona apple,ou=users,ou=system";

    public static final String FIONA_PASSWORD = "machine";

    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes(String sn, String cn)
    {
        Attributes attributes = new BasicAttributes();
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );

        return attributes;
    }

    /**
     * Creates an environment for JNDI LDAP connection.
     * 
     * @param principalDn
     *            Bind DN
     * @param password
     *            password
     */
    protected Hashtable getEnvironment(String principalDn, String password)
    {

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", principalDn );
        env.put( "java.naming.security.credentials", password );
        env.put( "java.naming.security.authentication", "simple" );

        return env;
    }

    /**
     * Create context and two person entries.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = getEnvironment( "uid=admin,ou=system", "secret" );
        ctx = new InitialLdapContext( env, null );
        assertNotNull( ctx );

        LdapContext users = (LdapContext) ctx.lookup( "ou=users" );

        // Create two persons
        Attributes tori = this.getPersonAttributes( "Amos", "Tori Amos" );
        users.createSubcontext( TORI_RDN, tori );
        Attributes fiona = this.getPersonAttributes( "Apple", "Fiona Apple" );
        fiona.put( "userPassword", FIONA_PASSWORD );
        users.createSubcontext( FIONA_RDN, fiona );
    }

    /**
     * Remove person entries and close context.
     */
    public void tearDown() throws Exception
    {
        LdapContext users = (LdapContext) ctx.lookup( "ou=users" );

        users.unbind( FIONA_RDN );
        users.unbind( TORI_RDN );
        ctx.close();
        ctx = null;
        super.tearDown();
    }

    /**
     * Check whether a user is only allowed to see his/her own entry, if located
     * below ou=users,ou=system.
     */
    public void testUserOnlySeesOwnUserEntry() throws Exception
    {

        Hashtable env = getEnvironment( FIONA_DN, FIONA_PASSWORD );
        LdapContext uCtx = new InitialLdapContext( env, null );
        assertNotNull( uCtx );

        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration enm = uCtx.search( "ou=users", "(objectClass=*)", ctls );
        int count = 0;
        while (enm.hasMore())
        {
            count++;
            SearchResult sr = (SearchResult) enm.next();
            assertEquals( FIONA_RDN, sr.getName() );
        }
        assertEquals( 1, count );

        enm.close();
        uCtx.close();
    }

    /**
     * Check whether admin is allowed to see both user entries
     */
    public void testAdminSeesAllUserEntries() throws Exception
    {

        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration enm = ctx.search( "ou=users", "(objectClass=*)", ctls );

        Set allNames = new HashSet();
        allNames.add( TORI_RDN );
        allNames.add( FIONA_RDN );
        int count = 0;

        while (enm.hasMore())
        {
            count++;
            SearchResult sr = (SearchResult) enm.next();
            assertTrue( allNames.contains( sr.getName() ) );
        }
        assertEquals( 2, count );

        enm.close();
    }
}