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
package org.apache.ldap.server.jndi;


import org.apache.ldap.server.AbstractServerTest;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;


/**
 * Adds extra code to perform operations as another user besides the admin user.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractMultiUserJndiTest extends AbstractServerTest
{
    protected ServerLdapContext sysRootAsNonAdminUser;


    /**
     * Set's up a context for an authenticated non-root user.
     *
     * @see org.apache.ldap.server.AbstractServerTest#setUp()
     */
    protected void setUp() throws Exception
    {
        // bring the system up
        super.setUp();

        // authenticate as akarasulu
        Hashtable env = new Hashtable( );
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.ServerContextFactory" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=akarasulu,ou=users,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "test" );
        InitialContext ictx = new InitialContext( env );
        sysRootAsNonAdminUser = ( ServerLdapContext ) ictx.lookup( "" );
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
        sysRootAsNonAdminUser = null;
    }
}
