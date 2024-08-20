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
package org.apache.directory.server.dns.service;


import java.util.List;

import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.i18n.I18n;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MonitorContext implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MonitorContext.class );

    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            try
            {
                DnsContext dnsContext = ( DnsContext ) session.getAttribute( getContextKey() );
                RecordStore store = dnsContext.getStore();
                List<ResourceRecord> records = dnsContext.getResourceRecords();

                StringBuilder sb = new StringBuilder();
                sb.append( "Monitoring context:" );
                sb.append( "\n\t" + "store:                     " + store );
                sb.append( "\n\t" + "records:                   " + records );

                LOG.debug( sb.toString() );
            }
            catch ( Exception e )
            {
                // This is a monitor.  No exceptions should bubble up.
                LOG.error( I18n.err( I18n.ERR_37001_ERROR_IN_CONTEXT_MONITOR ), e );
            }
        }

        next.execute( session, message );
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
