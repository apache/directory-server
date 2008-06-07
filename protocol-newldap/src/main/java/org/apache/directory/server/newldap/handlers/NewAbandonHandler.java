/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.newldap.handlers;


import org.apache.directory.shared.ldap.message.AbandonRequest;
import org.apache.directory.shared.ldap.message.AbandonableRequest;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * AbandonRequest handler implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NewAbandonHandler extends LdapRequestHandler<AbandonRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( NewAbandonHandler.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    
    /* (non-Javadoc)
     * @see org.apache.mina.handler.demux.MessageHandler#messageReceived(
     * org.apache.mina.common.IoSession, java.lang.Object)
     */
    public void messageReceived( IoSession session, AbandonRequest request ) throws Exception
    {
        int abandonedId = request.getAbandoned();

        if ( abandonedId < 0 )
        {
            return;
        }

        AbandonableRequest abandonedRequest = getOutstandingRequest( session, abandonedId );

        if ( abandonedRequest == null )
        {
            if ( LOG.isWarnEnabled() )
            {
                LOG.warn( "{}: Cannot find outstanding request {} to abandon.", session, request.getAbandoned() );
            }
            
            return;
        }

        abandonedRequest.abandon();
        
        if ( IS_DEBUG )
        {
            LOG.debug( "{}: Request {} was successfully flagged as abandoned.", abandonedRequest );
        }
    }
}
