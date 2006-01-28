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
 * @todo explain procrun behavoir
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ProcrunBootstrapper extends Bootstrapper
{
    private final static Logger log = LoggerFactory.getLogger( ProcrunBootstrapper.class );
    
    
    // -----------------------------------------------------------------------
    // Procrun Entry Points
    // -----------------------------------------------------------------------
    
    
    public static void prunsrvStart( String[] args )
    {
        log.debug( "prunsrvStart(String[]) called" );
        
        if ( log.isDebugEnabled() )
        {
            log.debug( "prunsrvStart(String[]) recieved args:" );
            for ( int ii = 0; ii < args.length; ii++ )
            {
                log.debug( "args[" + ii + "] = " + args[ii] );
            }
        }

        try
        {
            log.debug( "prunsrvStart(String[]) initializing Bootstrapper ... )" );
            ProcrunBootstrapper instance = new ProcrunBootstrapper();
            instance.setInstallationLayout( args[0] );
            instance.setParentLoader( Bootstrapper.class.getClassLoader() );

            log.debug( "prunsrvStart(String[]) calling callInit()" );
            instance.callInit( shift( args, 1 ) );
            log.debug( "prunsrvStart(String[]) calling callStart()" );
            instance.callStart( false );

            log.debug( "prunsrvStart(String[]) block waitForShutdown()" );
            instance.waitForShutdown();
            log.debug( "prunsrvStart(String[]) returned from waitForShutdown()" );

            log.debug( "prunsrvStart(String[]) calling callStop()" );
            instance.callStop( shift( args, 1 ) );
            log.debug( "prunsrvStart(String[]) calling callDestroy()" );
            instance.callDestroy();
        }
        catch ( Throwable t )
        {
            log.error( "Encountered error in prunsrvStart(String[])", t );
            System.exit( ExitCodes.UNKNOWN );
        }
    }


    public static void prunsrvStop( String[] args )
    {
        log.debug( "prunsrvStop(String[]) called" );
        if ( log.isDebugEnabled() )
        {
            log.debug( "prunsrvStop(String[]) recieved args:" );
            for ( int ii = 0; ii < args.length; ii++ )
            {
                log.debug( "args[" + ii + "] = " + args[ii] );
            }
        }

        try
        {
            log.debug( "prunsrvStop(String[]) initializing Bootstrapper ... )" );
            ProcrunBootstrapper instance = new ProcrunBootstrapper();
            instance.setInstallationLayout( args[0] );
            instance.setParentLoader( Bootstrapper.class.getClassLoader() );
            instance.sendShutdownCommand();
        }
        catch ( Throwable t )
        {
            log.error( "Encountered error in prunsrvStop(String[])", t );
            System.exit( ExitCodes.UNKNOWN );
        }
    }
}
