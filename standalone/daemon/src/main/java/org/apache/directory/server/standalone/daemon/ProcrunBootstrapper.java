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
    

    public static void prunsrvStart( String[] args )
    {
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

        log.debug( "prunsrvStart(String[]) creating BootstrappedApplication ... )" );
        BootstrappedApplication application = new BootstrappedApplication( args[0], 
            Thread.currentThread().getContextClassLoader() );

        log.debug( "prunsrvStart(String[]) invoking application.callInit())" );
        application.callInit();
        log.debug( "prunsrvStart(String[]) invoking bootstrapper.callStart())" );
        application.callStart();
        
        while( true )
        {
            try
            {
                Thread.sleep( 2000 );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
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

        log.debug( "prunsrvStop(String[]) creating BootstrappedApplication ... )" );
        BootstrappedApplication application = new BootstrappedApplication( args[0], 
            Thread.currentThread().getContextClassLoader() );
        
        log.debug( "prunsrvStop(String[]) invoking application.callStop())" );
        application.callStop();
        log.debug( "prunsrvStop(String[]) invoking application.callDestroy())" );
        application.callDestroy();
    }
}
