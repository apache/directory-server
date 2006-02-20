/*
 * Copyright (c) 2004 Solarsis Group LLC.
 *
 * Licensed under the Open Software License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://opensource.org/licenses/osl-2.1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.directory.server;


import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;


/**
 * Tests the effects of using a bad dn in various operations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BadDnTest extends AbstractServerTest
{
    /**
     * Bind as a user.
     */
    public LdapContext bind( String bindDn, String password ) throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", bindDn );
        env.put( "java.naming.security.credentials", password );
        env.put( "java.naming.security.authentication", "simple" );

        LdapContext ctx = new InitialLdapContext( env, null );
        assertNotNull( ctx );
        return ctx;
    }


    /**
     * Test with bindDn which is not even found under any namingContext of the
     * server.
     */
    public void testBadBindDnNotInContext() throws Exception
    {
        try
        {
            bind( "cn=bogus", "blah" );
            fail( "should never get here due to a " );
        }
        catch ( AuthenticationException e )
        {
        }
    }


    /**
     * Test with bindDn that is under a naming context but points to non-existant user.
     * @todo make this pass: see http://issues.apache.org/jira/browse/DIREVE-339
     */
    //    public void testBadBindDnMalformed() throws Exception
    //    {
    //        try
    //        {
    //            bind( "system", "blah" );
    //            fail( "should never get here due to a " );
    //        }
    //        catch ( InvalidNameException e ){}
    //    }

    /**
     * Test with bindDn that is under a naming context but points to non-existant user.
     */
    public void testBadBindDnInContext() throws Exception
    {
        try
        {
            bind( "cn=bogus,ou=system", "blah" );
            fail( "should never get here due to a " );
        }
        catch ( AuthenticationException e )
        {
        }
    }
}
