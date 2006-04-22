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


import org.apache.mina.handler.chain.IoHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Domain Name Service (DNS) Protocol (RFC 1034, 1035)
 */
public class DomainNameServiceChain extends IoHandlerChain
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( DomainNameServiceChain.class );


    public DomainNameServiceChain()
    {
        if ( log.isDebugEnabled() )
        {
            addLast( "monitorRequest", new MonitorRequest() );
        }

        addLast( "getResourceRecords", new GetResourceRecords() );

        if ( log.isDebugEnabled() )
        {
            addLast( "monitorContext", new MonitorContext() );
        }

        addLast( "buildReply", new BuildReply() );

        if ( log.isDebugEnabled() )
        {
            addLast( "monitorReply", new MonitorReply() );
        }
    }
}
