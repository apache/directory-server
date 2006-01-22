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

import org.apache.directory.server.standalone.daemon.InstallationLayout;
import org.apache.ldap.server.configuration.MutableServerStartupConfiguration;
import org.apache.ldap.server.configuration.SyncConfiguration;
import org.apache.ldap.server.jndi.ServerContextFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;


/**
 * DirectoryServer bean used by both the daemon code and by the ServerMain here.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DirectoryServer
{
    private static final Logger log = LoggerFactory.getLogger( DirectoryServer.class );
    private Properties env;


    public void init( InstallationLayout install ) throws Exception
    {
        long startTime = System.currentTimeMillis();
        MutableServerStartupConfiguration cfg;

        if ( install != null )
        {
            log.info( "server: loading settings from {}", install.getConfigurationFile() );
            ApplicationContext factory = null;
            factory = new FileSystemXmlApplicationContext( install.getConfigurationFile().toURL().toString() );
            cfg = ( MutableServerStartupConfiguration ) factory.getBean( "configuration" );
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
        cfg.setWorkingDirectory( install.getPartitionsDirectory() );
        env.putAll( cfg.toJndiEnvironment() );
        new InitialDirContext( env );

        if (log.isInfoEnabled())
        {
            log.info( "server: started in {} milliseconds", ( System.currentTimeMillis() - startTime ) + "");
        }
    }
    
    
    public void synch() throws Exception
    {
        env.putAll( new SyncConfiguration().toJndiEnvironment() );
        new InitialDirContext( env );
    }
}
