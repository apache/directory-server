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


import org.apache.directory.server.protocol.shared.chain.impl.ChainBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Domain Name Service (DNS) Protocol (RFC 1034, 1035)
 */
public class DomainNameServiceChain extends ChainBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( DomainNameServiceChain.class );


    public DomainNameServiceChain()
    {
        addCommand( new DnsExceptionHandler() );

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorRequest() );
        }

        addCommand( new GetResourceRecords() );

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorContext() );
        }

        addCommand( new BuildReply() );

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorReply() );
        }
    }
}
