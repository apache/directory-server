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
package org.apache.directory.server.ldap.handlers;


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.internal.InternalAddRequest;
import org.apache.directory.shared.ldap.message.internal.InternalLdapResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An LDAP add operation {@link InternalAddRequest} handler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AddHandler extends LdapRequestHandler<InternalAddRequest>
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AddHandler.class );
    
    
    /**
     * {@inheritDoc}
     */
    public void handle( LdapSession session, InternalAddRequest req ) 
    {
        LOG.debug( "Handling request: {}", req );
        InternalLdapResult result = req.getResultResponse().getLdapResult();

        try
        {
            // Call the underlying layer to inject the new entry 
            CoreSession coreSession = session.getCoreSession();
            coreSession.add( req );

            // If success, here now, otherwise, we would have an exception.
            result.setResultCode( ResultCodeEnum.SUCCESS );
            
            // Write the AddResponse message
            session.getIoSession().write( req.getResultResponse() );
        }
        catch ( Exception e )
        {
            handleException( session, req, e );
        }
    }
}
