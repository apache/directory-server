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
package org.apache.eve.jndi;


import java.util.Hashtable;
import java.io.File;
import javax.naming.ldap.LdapContext;
import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;


/**
 * A simple testcase for testing JNDI provider functionality.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractJndiTest extends TestCase
{
    /** the context root for the system partition */
    protected LdapContext sysRoot;


    /**
     * Get's the initial context factory for the provider's ou=system context
     * root.
     *
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
        env.put( EveContextFactory.WKDIR_ENV, "target/eve" );
        InitialContext initialContext = new InitialContext( env );
        sysRoot = ( LdapContext ) initialContext.lookup( "" );
    }


    /**
     * Sets the system context root to null.
     *
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        Hashtable env = new Hashtable();
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
        env.put( EveContextFactory.SHUTDOWN_OP_ENV, "" );

        try
        {
            InitialContext initialContext = new InitialContext( env );
        }
        catch( Exception e )
        {

        }

        sysRoot = null;
        File file = new File( "target/eve" );
        FileUtils.deleteDirectory( file );
    }
}
