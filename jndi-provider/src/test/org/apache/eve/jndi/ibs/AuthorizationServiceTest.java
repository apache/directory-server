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
package org.apache.eve.jndi.ibs;


import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attributes;

import org.apache.eve.jndi.EveLdapContext;
import org.apache.eve.jndi.AbstractJndiTest;
import org.apache.eve.exception.EveNoPermissionException;
import org.apache.ldap.common.message.LockableAttributesImpl;


/**
 * Tests the Authorization service to make sure it is enforcing policies
 * correctly.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthorizationServiceTest extends AbstractJndiTest
{
    EveLdapContext sysRootAsNonRootUser;


    /**
     * Set's up a context for an authenticated non-root user.
     *
     * @see AbstractJndiTest#setUp()
     */
    protected void setUp() throws Exception
    {
        // bring the system up
        super.setUp();

        // authenticate as akarasulu
        Hashtable env = new Hashtable( );
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=akarasulu,ou=users,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "test" );
        InitialContext ictx = new InitialContext( env );
        sysRootAsNonRootUser = ( EveLdapContext ) ictx.lookup( "" );
    }


    /**
     * Makes sure the admin cannot delete the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testNoDeleteOnAdminByAdmin() throws NamingException
    {
        try
        {
            sysRoot.destroySubcontext( "uid=admin" );
            fail( "admin should not be able to delete his account" );
        }
        catch ( EveNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure a non-admin user cannot delete the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testNoDeleteOnAdminByNonAdmin() throws NamingException
    {
        try
        {
            sysRootAsNonRootUser.destroySubcontext( "uid=admin" );
            fail( sysRootAsNonRootUser.getPrincipal().getDn()
                    + " should not be able to delete his account" );
        }
        catch ( EveNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure the admin cannot rename the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testNoRdnChangesOnAdminByAdmin() throws NamingException
    {
        try
        {
            sysRoot.rename( "uid=admin", "uid=alex" );
            fail( "admin should not be able to rename his account" );
        }
        catch ( EveNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure a non-admin user cannot rename the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testNoRdnChangesOnAdminByNonAdmin() throws NamingException
    {
        try
        {
            sysRootAsNonRootUser.rename( "uid=admin", "uid=alex" );
            fail( "admin should not be able to rename his account" );
        }
        catch ( EveNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure the admin cannot rename the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testModifyOnAdminByAdmin() throws NamingException
    {
        Attributes attributes = new LockableAttributesImpl();
        attributes.put( "userPassword", "replaced" );
        sysRoot.modifyAttributes( "uid=admin", DirContext.REPLACE_ATTRIBUTE, attributes );
        Attributes newAttrs = sysRoot.getAttributes( "uid=admin" );
        assertEquals( "replaced", newAttrs.get( "userPassword" ).get() );
    }


//    /**
//     * Makes sure the a non-admin user cannot rename the admin account.
//     */
//    public void testModifyOnAdminByNonAdmin()
//    {
//        Attributes attributes = new LockableAttributesImpl();
//        attributes.put( "userPassword", "replaced" );
//
//        try
//        {
//            sysRootAsNonRootUser.modifyAttributes( "uid=admin",
//                    DirContext.REPLACE_ATTRIBUTE, attributes );
//            fail( sysRootAsNonRootUser.getPrincipal().getDn() +
//                    " should not be able to modify attributes on admin" );
//        } catch( Exception e ) { }
//    }
}
