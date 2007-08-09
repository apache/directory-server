/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server;


import java.io.File;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.daemon.DaemonApplication;
import org.apache.directory.daemon.InstallationLayout;
import org.apache.directory.server.configuration.MutableServerStartupConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.configuration.SyncConfiguration;
import org.apache.directory.server.jndi.ServerContextFactory;

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
public class Service implements DaemonApplication
{
    private static final Logger log = LoggerFactory.getLogger( Service.class );
    private Properties env;
    private Thread workerThread = null;
    private SynchWorker worker = new SynchWorker();
    private MutableServerStartupConfiguration cfg;
    private boolean startNoWait = false;


    public void init( InstallationLayout install, String[] args ) throws Exception
    {
        printBanner();
        long startTime = System.currentTimeMillis();

//        if ( install != null )
//        {
//            log.info( "server: loading settings from ", install.getConfigurationFile() );
//            ApplicationContext factory = null;
//            factory = new FileSystemXmlApplicationContext( install.getConfigurationFile().toURL().toString() );
//            cfg = ( MutableServerStartupConfiguration ) factory.getBean( "configuration" );
//            env = ( Properties ) factory.getBean( "environment" );
//        }
//        else if ( args.length > 0 && new File( args[0] ).exists() ) // hack that takes server.xml file argument
        if ( args.length > 0 && new File( args[0] ).exists() ) // hack that takes server.xml file argument
        {
            log.info( "server: loading settings from ", args[0] );
            ApplicationContext factory = null;
            factory = new FileSystemXmlApplicationContext( new File( args[0] ).toURL().toString() );
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

        if ( install != null )
        {
            cfg.setWorkingDirectory( install.getPartitionsDirectory() );
        }

        env.putAll( cfg.toJndiEnvironment() );
        new InitialDirContext( env );

        if ( cfg.getSynchPeriodMillis() > 0 )
        {
            workerThread = new Thread( worker, "SynchWorkerThread" );
        }
        
        if ( log.isInfoEnabled() )
        {
            log.info( "server: started in {} milliseconds", ( System.currentTimeMillis() - startTime ) + "" );
        }
    }


    public void synch() throws Exception
    {
        env.putAll( new SyncConfiguration().toJndiEnvironment() );
        new InitialDirContext( env );
    }


    public void start()
    {
        if ( workerThread != null )
        {
            workerThread.start();
        }
        return;
    }


    public void stop( String[] args ) throws Exception
    {
        if ( workerThread != null )
        {
            worker.stop = true;
            synchronized ( worker.lock )
            {
                worker.lock.notify();
            }
    
            while ( startNoWait && workerThread.isAlive() )
            {
                log.info( "Waiting for SynchWorkerThread to die." );
                workerThread.join( 500 );
            }
        }

        env.putAll( new ShutdownConfiguration().toJndiEnvironment() );
        new InitialDirContext( env );
    }


    public void destroy()
    {
    }

    
    class SynchWorker implements Runnable
    {
        Object lock = new Object();
        boolean stop = false;


        public void run()
        {
            while ( !stop )
            {
                synchronized ( lock )
                {
                    try
                    {
                        lock.wait( cfg.getSynchPeriodMillis() );
                    }
                    catch ( InterruptedException e )
                    {
                        log.warn( "SynchWorker failed to wait on lock.", e );
                    }
                }

                try
                {
                    synch();
                }
                catch ( Exception e )
                {
                    log.error( "SynchWorker failed to synch directory.", e );
                }
            }
        }
    }

    public static final String BANNER = "           _                     _          ____  ____   \n"
        + "          / \\   _ __   __ _  ___| |__   ___|  _ \\/ ___|  \n"
        + "         / _ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\ | | \\___ \\   \n"
        + "        / ___ \\| |_) | (_| | (__| | | |  __/ |_| |___) |  \n"
        + "       /_/   \\_\\ .__/ \\__,_|\\___|_| |_|\\___|____/|____/   \n"
        + "               |_|                                                               \n";


    public static void printBanner()
    {
        System.out.println( BANNER );
    }
}
