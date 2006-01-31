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
package org.apache.directory.server.standalone.daemon;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The bootstrapper used by the jsvc process manager.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JsvcBootstrapper extends Bootstrapper
{
    private final static Logger log = LoggerFactory.getLogger( JsvcBootstrapper.class );
    private boolean isListenerShuttingDown = false;
    private boolean isDaemonShuttingDown = false;
    private Thread thread;

    
    public void init( String[] args )
    {
        if ( log.isDebugEnabled() )
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "init(String[]) called with args: \n" );
            for ( int ii = 0; ii < args.length; ii++ )
            {
                buf.append( "\t" ).append( args[ii] ).append( "\n" );
            }
            log.debug( buf.toString() );
        }

        setInstallationLayout( args[0] );
        setParentLoader( Thread.currentThread().getContextClassLoader() );
        callInit( shift( args, 1 ) );
        thread = new Thread( new ShutdownListener(), "ShutdownListenerThread" );
    }
    
    
    public void start()
    {
        log.debug( "start() called" );
        callStart();
        thread.start();
    }


    public void stop() throws Exception
    {
        log.debug( "stop() called using regular shutdown with signals" );
        
        // Bad construct here since there is no synchronization but there is
        // no really good way to do this with the way threads are setup.  So
        // both the listener thread and the daemon thread may try to shutdown
        // the server.  It's possible that both will try at the same time in
        // which case there may be issues.  However the chances of this are 
        // very small.  For all practical purposes this will work just fine.
        // And so what if they try to shutdown at the same time.  One thread
        // will just get an exception due to a DeadContext.
        
        if ( ! isListenerShuttingDown )
        {
            isDaemonShuttingDown = true;
            callStop( EMPTY_STRARRAY );
        }
    }


    public void destroy()
    {
        log.debug( "destroy() called" );
        callDestroy();
    }
    
    
    class ShutdownListener implements Runnable
    {
        public void run()
        {
            waitForShutdown();
            log.debug( "ShutdownListener came out of waitForShutdown" );
            if ( ! isDaemonShuttingDown )
            {
                isListenerShuttingDown = true;
                log.debug( "ShutdownListener will invoke callStop(String[])." );
                callStop( EMPTY_STRARRAY );
                log.debug( "ShutdownListener will invoke callDestroy()." );
                callDestroy();
                log.debug( "ShutdownListener will exit the system." );
                System.exit( 0 );
            }
        }
    }
}
