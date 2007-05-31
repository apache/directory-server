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
package org.apache.directory.server.ldap.support.bind;


import java.util.Set;

import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChainGuard implements IoHandlerCommand
{
    private static final Logger log = LoggerFactory.getLogger( ChainGuard.class );


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        BindRequest request = ( BindRequest ) message;
        LdapResult result = request.getResultResponse().getLdapResult();

        if ( log.isDebugEnabled() )
        {
            log.debug( "Is simple:       " + request.isSimple() );
            log.debug( "SASL mechanism:  " + request.getSaslMechanism() );
            log.debug( "Credentials:     " + request.getCredentials() );
        }

        // Guard clause:  LDAP version 3
        if ( !request.getVersion3() )
        {
            result.setResultCode( ResultCodeEnum.PROTOCOL_ERROR );
            result.setErrorMessage( "Only LDAP v3 is supported." );
            session.write( request.getResultResponse() );
            return;
        }

        Set supportedMechanisms = ( Set ) session.getAttribute( "supportedMechanisms" );

        // Guard clause:  Reject SIMPLE mechanism.
        if ( request.isSimple() && !supportedMechanisms.contains( "SIMPLE" ) )
        {
            result.setResultCode( ResultCodeEnum.STRONG_AUTH_REQUIRED );
            result.setErrorMessage( "Simple binds are disabled." );
            session.write( request.getResultResponse() );
            return;
        }

        // Guard clause:  Reject unsupported SASL mechanisms.
        if ( !( request.isSimple() || supportedMechanisms.contains( request.getSaslMechanism() ) ) )
        {
            result.setResultCode( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
            result.setErrorMessage( request.getSaslMechanism() + " is not a supported mechanism." );
            session.write( request.getResultResponse() );
            return;
        }

        /**
         * We now have a canonicalized authentication mechanism for this session,
         * suitable for use in Hashed Adapter's, aka Demux HashMap's.
         */
        if ( request.isSimple() )
        {
            session.setAttribute( "sessionMechanism", "SIMPLE" );
        }
        else
        {
            session.setAttribute( "sessionMechanism", request.getSaslMechanism() );
        }

        next.execute( session, message );
    }
}
