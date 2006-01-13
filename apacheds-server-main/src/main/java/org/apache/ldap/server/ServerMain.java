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

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;

import org.apache.ldap.server.configuration.MutableServerStartupConfiguration;
import org.apache.ldap.server.configuration.ServerStartupConfiguration;
import org.apache.ldap.server.configuration.SyncConfiguration;
import org.apache.ldap.server.jndi.ServerContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/**
 * The command line main for the server.  Warning this used to be a simple test
 * case so there really is not much here.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerMain
{
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
    
    /**
     * Takes a single argument, an optional properties file to load with server
     * startup settings.
     *
     * @param args the arguments
     */
    public static void main( String[] args ) throws Exception
    {
        long startTime = System.currentTimeMillis();
        Properties env;
        ServerStartupConfiguration cfg;

        if ( args.length > 0 )
        {
            log.info( "server: loading settings from {}", args[0] );
            ApplicationContext factory = new FileSystemXmlApplicationContext( args[0] );
            cfg = ( ServerStartupConfiguration ) factory.getBean( "configuration" );
            env = ( Properties ) factory.getBean( "environment" );
        }
        else
        {
            log.info( "server: using default settings ..." );
            env = new Properties();
            cfg = new MutableServerStartupConfiguration();
        }

        env.setProperty( Context.PROVIDER_URL, "ou=system" );
        env.setProperty( Context.INITIAL_CONTEXT_FACTORY, ServerContextFactory.class.getName() );
        env.putAll( cfg.toJndiEnvironment() );

        new InitialDirContext( env );

        if (log.isInfoEnabled())
        {
            log.info( "server: started in {} milliseconds",
                    ( System.currentTimeMillis() - startTime ) + "");
        }

        while ( true )
        {
            try
            {
                // this is a big time cludge for now to just play
                Thread.sleep( 20000 );
            }
            catch ( InterruptedException e )
            {
            }

            env.putAll( new SyncConfiguration().toJndiEnvironment() );
            new InitialDirContext( env );
        }
    }
}
