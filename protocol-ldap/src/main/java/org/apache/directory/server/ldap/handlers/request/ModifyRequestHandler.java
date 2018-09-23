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
package org.apache.directory.server.ldap.handlers.request;


import org.apache.directory.api.ldap.codec.decorators.ModifyResponseDecorator;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.LdapRequestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A single reply MessageReceived handler for {@link ModifyRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyRequestHandler extends LdapRequestHandler<ModifyRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( ModifyRequestHandler.class );


    /**
     * {@inheritDoc}
     */
    public void handle( LdapSession session, ModifyRequest modifyRequest )
    {
        LOG.debug( "Handling request : {}", modifyRequest );
        
        ModifyResponse modifyResponse = ( ModifyResponse ) modifyRequest.getResultResponse();
        
        LdapResult result = modifyResponse.getLdapResult();

        try
        {
            // Call the underlying layer to delete the entry
            CoreSession coreSession = session.getCoreSession();
            coreSession.modify( modifyRequest );

            // If success, here now, otherwise, we would have an exception.
            result.setResultCode( ResultCodeEnum.SUCCESS );

            // Write the DeleteResponse message
            session.getIoSession().write( new ModifyResponseDecorator( getLdapApiService(), modifyResponse ) );
        }
        catch ( Exception e )
        {
            handleException( session, modifyRequest, new ModifyResponseDecorator( getLdapApiService(), modifyResponse ), e );
        }
    }
}