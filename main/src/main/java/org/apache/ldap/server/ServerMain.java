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


import java.util.Properties;
import java.io.File;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.apache.ldap.common.util.PropertiesUtils;
import org.apache.ldap.server.jndi.EnvKeys;
import org.apache.seda.listener.AvailablePortFinder;


/**
 * The command line main for the server.  Warning this used to be a simple test
 * case so there really is not much here.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerMain
{
    /** the default LDAP port to use */
    private static final int LDAP_PORT = 389;

    /**
     * Takes a single argument, an optional properties file to load with server
     * startup settings.
     *
     * @param args the arguments
     */
    public static void main( String[] args )
    {
        long startTime = System.currentTimeMillis();
        Properties env;

        if ( args.length > 0 )
        {
            System.out.println( "server: loading properties from " + args[0] );
            env = PropertiesUtils.getProperties( new File( args[0] ) );
        }
        else
        {
            System.out.println( "server: using default properties ..." );
            env = new Properties();
        }

        if ( ! env.containsKey( EnvKeys.LDAP_PORT ) )
        {
            int port = LDAP_PORT;

            if ( ! AvailablePortFinder.available( port ) )
            {
                port = AvailablePortFinder.getNextAvailable( 1024 );
                System.out.println( "server: standard ldap port " + LDAP_PORT
                        + " is not available, using " + port + " instead" );
            }

            env.setProperty( EnvKeys.LDAP_PORT, String.valueOf( port ) );
        }

        env.setProperty( Context.PROVIDER_URL, "ou=system" );
        env.setProperty( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.ServerContextFactory" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
        }

        System.out.println( "server: started in "
                + ( System.currentTimeMillis() - startTime )
                + " milliseconds");

        while ( true )
        {
            try
            {
                // this is a big time cludge for now to just play
                Thread.sleep( 20000 );

                try
                {
                    env.setProperty( EnvKeys.SYNC, "true" );
                    new InitialDirContext( env );
                }
                catch ( NamingException e )
                {
                    e.printStackTrace();
                }
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        }
    }
}
