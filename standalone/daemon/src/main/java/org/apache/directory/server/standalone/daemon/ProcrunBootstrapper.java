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
 * The bootstrapper used for procrun services on windows platforms.  This
 * class contains static methods invoked by the prunsrv service manager.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ProcrunBootstrapper
{
    private final static Logger log = LoggerFactory.getLogger( ProcrunBootstrapper.class );
    private static final String[] EMPTY_STRARRAY = new String[0];
    

    public static void prunsrvStart( String[] args )
    {
    	ClassLoader system = Thread.currentThread().getContextClassLoader();
    	
        if ( log.isDebugEnabled() )
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "ENTERING ==> prunsrvStart(String[]):\n" );
            for ( int ii = 0; ii < args.length; ii++ )
            {
                buf.append( "\targs[" ).append( ii ).append( "] = " ).append( args[ii] ).append( "\n" );
            }
        }

        if ( args == null || args.length < 1 )
        {
            log.error( "Args were null or less than 1: need home directory.  Shutting down!" );
            System.exit( ExitCodes.BAD_ARGUMENTS );
        }

        log.debug( "prunsrvStart(String[]) creating LifecycleInvoker ... )" );
        LifecycleInvoker invoker = new LifecycleInvoker( args[0], system );

        log.debug( "prunsrvStart(String[]) invoking application.callInit(String[]))" );
        try
        {
	        if ( args.length > 1 )
	        {
	            String[] shifted = new String[args.length-1];
	            System.arraycopy( args, 1, shifted, 0, shifted.length );
	            invoker.callInit( shifted );
	        }
	        else
	        {
	            invoker.callInit( EMPTY_STRARRAY );
	        }
        }
        catch ( Throwable t )
        {
        	log.error( "Failed while calling invoker.callInit(String[])", t );
        }
        
        log.debug( "prunsrvStart(String[]) invoking bootstrapper.callStart())" );
        try
        {
        	invoker.callStart( false ); // must block on start (let the app decide how)
        }
        catch( Throwable t )
        {
        	log.error( "Failed while calling invoker.callStart(String[])", t );
        }
    }

    
    public static void prunsrvStop( String[] args )
    {
        if ( log.isDebugEnabled() )
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "ENTERING ==> prunsrvStop(String[]):\n" );
            for ( int ii = 0; ii < args.length; ii++ )
            {
                buf.append( "\targs[" ).append( ii ).append( "] = " ).append( args[ii] ).append( "\n" );
            }
        }
        
        if ( args == null || args.length < 1 )
        {
            log.error( "Args were null or less than 1: need home directory.  Shutting down!" );
            System.exit( ExitCodes.BAD_ARGUMENTS );
        }

        log.debug( "prunsrvStop(String[]) creating LifecycleInvoker ... )" );
        LifecycleInvoker application = new LifecycleInvoker( args[0], 
            Thread.currentThread().getContextClassLoader() );
        
        log.debug( "prunsrvStop(String[]) invoking application.callStop(String[]))" );
        if ( args.length > 1 )
        {
            String[] shifted = new String[args.length-1];
            System.arraycopy( args, 1, shifted, 0, shifted.length );
            application.callStop( shifted );
        }
        else
        {
            application.callStop( EMPTY_STRARRAY );
        }
        log.debug( "prunsrvStop(String[]) invoking application.callDestroy())" );
        application.callDestroy();
    }
}
