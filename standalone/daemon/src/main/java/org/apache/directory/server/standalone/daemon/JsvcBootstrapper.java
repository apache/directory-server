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
        callInit();

//        if ( args.length > 1 )
//        {
//            String[] shifted = new String[args.length-1];
//            System.arraycopy( args, 1, shifted, 0, shifted.length );
//            setInstallationLayout( args[0] );
//            setParentLoader( Thread.currentThread().getContextClassLoader() );
//            callInit();
//        }
//        else
//        {
//            callInit( EMPTY_STRARRAY );
//        }
    }
    
    
    public void start()
    {
        log.debug( "start() called" );
        callStart();
//      callStart( true );
    }


    public void stop() throws Exception
    {
        log.debug( "stop() called" );
        callStop();
        //callStop( EMPTY_STRARRAY );
    }


    public void destroy()
    {
        log.debug( "destroy() called" );
        callDestroy();
    }
}
