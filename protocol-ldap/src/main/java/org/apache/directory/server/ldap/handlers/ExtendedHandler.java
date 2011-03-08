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
package org.apache.directory.server.ldap.handlers;


import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.model.message.ExtendedRequest;
import org.apache.directory.shared.ldap.model.message.ExtendedResponse;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;


/**
* A single reply handler for {@link ExtendedRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtendedHandler extends LdapRequestHandler<ExtendedRequest<ExtendedResponse>>
{
    public void handle( LdapSession session, ExtendedRequest<ExtendedResponse> req ) throws Exception
    {
        ExtendedOperationHandler<ExtendedRequest<ExtendedResponse>, ExtendedResponse> handler = getLdapServer().getExtendedOperationHandler( req.getRequestName() );

        if ( handler == null )
        {
            // As long as no extended operations are implemented, send appropriate
            // error back to the client.
            String msg = "Unrecognized extended operation EXTENSION_OID: " + req.getRequestName();
            LdapResult result = req.getResultResponse().getLdapResult();
            result.setResultCode( ResultCodeEnum.PROTOCOL_ERROR );
            result.setErrorMessage( msg );
            session.getIoSession().write( req.getResultResponse() );
            return;
        }

        try
        {
            handler.handleExtendedOperation( session, req );
        }
        catch ( Exception e )
        {
            LdapResult result = req.getResultResponse().getLdapResult();
            result.setResultCode( ResultCodeEnum.OTHER );
            result.setErrorMessage( ResultCodeEnum.OTHER
                + ": Extended operation handler for the specified EXTENSION_OID (" + req.getRequestName()
                + ") has failed to process your request:\n" + ExceptionUtils.getStackTrace( e ) );
            ExtendedResponse resp = req.getResultResponse();
            session.getIoSession().write( resp );
        }
    }
}