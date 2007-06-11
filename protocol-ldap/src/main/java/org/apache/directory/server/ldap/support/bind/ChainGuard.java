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
        BindRequest bindRequest = ( BindRequest ) message;

        @SuppressWarnings( "unchecked" )
        Set<String> supportedMechanisms = ( Set<String> ) session.getAttribute( "supportedMechanisms" );

        // Guard clause:  Reject unsupported SASL mechanisms.
        if ( !supportedMechanisms.contains( bindRequest.getSaslMechanism() ) )
        {
            log.error( "Bind error : {} mechanism not supported. Please check the server.xml configuration file (supportedMechanisms field)", 
                bindRequest.getSaslMechanism() );

            LdapResult bindResult = bindRequest.getResultResponse().getLdapResult();
            bindResult.setResultCode( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
            bindResult.setErrorMessage( bindRequest.getSaslMechanism() + " is not a supported mechanism." );
            session.write( bindRequest.getResultResponse() );
            return;
        }

        /**
         * We now have a canonicalized authentication mechanism for this session,
         * suitable for use in Hashed Adapter's, aka Demux HashMap's.
         */
        session.setAttribute( "sessionMechanism", bindRequest.getSaslMechanism() );

        next.execute( session, message );
    }
}
