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
package org.apache.directory.server.ldap.handlers.request;


import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.LdapRequestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An LDAP add operation {@link AddRequest} MessageReceived handler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddRequestHandler extends LdapRequestHandler<AddRequest>
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AddRequestHandler.class );


    /**
     * {@inheritDoc}
     */
    public void handle( LdapSession session, AddRequest addRequest )
    {
        LOG.debug( "Handling request: {}", addRequest );
        AddResponse addResponse = ( AddResponse ) addRequest.getResultResponse();

        try
        {
            // Call the underlying layer to inject the new entry 
            CoreSession coreSession = session.getCoreSession();
            coreSession.add( addRequest );

            // If success, here now, otherwise, we would have an exception.
            LdapResult result = addResponse.getLdapResult();
            result.setResultCode( ResultCodeEnum.SUCCESS );

            // Write the AddResponse message
            session.getIoSession().write( addResponse );
        }
        catch ( Exception e )
        {
            handleException( session, addRequest, addResponse, e );
        }
    }
}
