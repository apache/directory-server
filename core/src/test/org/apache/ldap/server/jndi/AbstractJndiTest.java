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


import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.apseda.listener.AvailablePortFinder;
import org.apache.apseda.listener.AvailablePortFinder;


/**
 * A simple testcase for testing JNDI provider functionality.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractJndiTest extends TestCase
{
    /** the context root for the system partition */
    protected LdapContext sysRoot;

    /** flag whether to delete database files for each test or not */
    protected boolean doDelete = true;

    /** extra environment parameters that can be added before setUp */
    protected Hashtable extras = new Hashtable();

    /** extra environment parameters that can be added before setUp to override values */
    protected Hashtable overrides = new Hashtable();


    /**
     * Get's the initial context factory for the provider's ou=system context
     * root.
     *
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        doDelete( new File( "target" + File.separator + "eve" ) );

        extras.put( EnvKeys.LDAP_PORT,
                String.valueOf( AvailablePortFinder.getNextAvailable( 1024 ) ) );

        setSysRoot( "uid=admin,ou=system", "secret" );
    }


    /**
     * Deletes the Eve working directory.
     */
    protected void doDelete( File wkdir )
    {
        try 
        {
            if ( doDelete )
            {
                if ( wkdir.exists() )
                {
                    FileUtils.deleteDirectory( wkdir );
                }
            }
        }
        catch( IOException ioe )
        {
            ioe.printStackTrace();
        }
    }


    /**
     * Sets and returns the system root.  Values of user and password used to
     * set the respective JNDI properties.  These values can be overriden by the
     * overrides properties.
     *
     * @param user the username for authenticating as this user
     * @param passwd the password of the user
     * @return the sysRoot context which is also set
     * @throws NamingException if there is a failure of any kind
     */
    protected LdapContext setSysRoot( String user, String passwd ) throws NamingException
    {
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_PRINCIPAL, user );
        env.put( Context.SECURITY_CREDENTIALS, passwd );
        return setSysRoot( env );
    }


    /**
     * Sets the system root taking into account the extras and overrides
     * properties.  In between these it sets the properties for the working
     * directory, the provider URL and the JNDI InitialContexFactory to use.
     *
     * @param env an environment to use while setting up the system root.
     * @return the sysRoot context which is also set
     * @throws NamingException if there is a failure of any kind
     */
    protected LdapContext setSysRoot( Hashtable env ) throws NamingException
    {
        Hashtable envFinal = new Hashtable();
        envFinal.putAll( extras );
        envFinal.putAll( env );
        envFinal.put( Context.PROVIDER_URL, "ou=system" );
        envFinal.put( EnvKeys.WKDIR, "target" + File.separator + "eve" );
        envFinal.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.ServerContextFactory" );
        envFinal.putAll( overrides );
        return sysRoot = new InitialLdapContext( envFinal, null );
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
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.ServerContextFactory" );
        env.put( EnvKeys.SHUTDOWN, "" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        try { new InitialContext( env ); } catch( Exception e ) {}
        sysRoot = null;
    }
}
