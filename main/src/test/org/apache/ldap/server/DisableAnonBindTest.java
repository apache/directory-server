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


import org.apache.ldap.server.jndi.EnvKeys;

import javax.naming.*;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;


/**
 * A set of simple tests to make sure simple authentication is working as it
 * should.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DisableAnonBindTest extends AbstractServerTest
{
    /**
     * Cleans up old database files on creation.
     * @throws IOException 
     */
    public DisableAnonBindTest() throws IOException
    {
        doDelete( new File( "target" + File.separator + "server" ) );
    }


    /**
     * Customizes setup for each test case.
     *
     * @throws Exception
     */
    protected void setUp() throws Exception
    {
        if ( getName().equals( "testDisableAnonymousBinds" ) )
        {
            extras.put( EnvKeys.DISABLE_ANONYMOUS, "true" );
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

        InitialContext ctx = null;

        try
        {
            ctx = new InitialContext( env );

            fail( "If anonymous binds are disabled we should never get here!" );
        }
        catch ( NoPermissionException e )
        {
            assertNull( ctx );

            assertNotNull( e );
        }
    }
}
