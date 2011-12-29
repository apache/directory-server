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


import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.model.message.CompareRequest;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A single reply handler for {@link org.apache.directory.shared.ldap.model.message.CompareRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CompareHandler extends LdapRequestHandler<CompareRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( CompareHandler.class );


    /**
     * @see LdapRequestHandler#handle(LdapSession, org.apache.directory.shared.ldap.model.message.Request)
     */
    @Override
    public void handle( LdapSession session, CompareRequest req )
    {
        LOG.debug( "Handling compare request while ignoring referrals: {}", req );
        LdapResult result = req.getResultResponse().getLdapResult();

        try
        {
            if ( session.getCoreSession().compare( req ) )
            {
                result.setResultCode( ResultCodeEnum.COMPARE_TRUE );
            }
            else
            {
                result.setResultCode( ResultCodeEnum.COMPARE_FALSE );
            }

            result.setMatchedDn( req.getName() );
            session.getIoSession().write( req.getResultResponse() );
        }
        catch ( Exception e )
        {
            handleException( session, req, e );
        }
    }
}