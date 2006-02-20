/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.server.dns.service;


import org.apache.directory.server.protocol.shared.chain.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MonitorRequest extends MonitorMessage
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorRequest.class );


    public boolean execute( Context context ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            try
            {
                DnsContext dnsContext = ( DnsContext ) context;

                log.debug( monitorMessage( dnsContext.getRequest(), "request" ) );
            }
            catch ( Exception e )
            {
                // This is a monitor.  No exceptions should bubble up.
                log.error( "Error in request monitor", e );
            }
        }

        return CONTINUE_CHAIN;
    }
}
