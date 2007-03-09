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


import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.BindResponse;
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
public class ReturnSuccess implements IoHandlerCommand
{
    private static final Logger log = LoggerFactory.getLogger( ReturnSuccess.class );

    private static final String SASL_STATE = "saslState";

    // Server has bound, specifically the bind requires QoP processing on all messages (similar to SSL).
    private static final Boolean SASL_STATE_BOUND = true;


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        /*
         * We have now both authenticated the client and retrieved a JNDI context for them.
         * We can return a success message to the client.
         */
        BindRequest request = ( BindRequest ) message;
        LdapResult result = request.getResultResponse().getLdapResult();

        byte[] tokenBytes = ( byte[] ) session.getAttribute( "saslCreds" );

        result.setResultCode( ResultCodeEnum.SUCCESS );
        BindResponse response = ( BindResponse ) request.getResultResponse();
        response.setServerSaslCreds( tokenBytes );
        session.write( response );

        log.debug( "Returned SUCCESS message." );

        String sessionMechanism = ( String ) session.getAttribute( "sessionMechanism" );

        /*
         * This is how we tell the SaslFilter to turn on.
         */
        if ( sessionMechanism.equals( "DIGEST-MD5" ) || sessionMechanism.equals( "GSSAPI" ) )
        {
            log.debug( "Enabling SaslFilter to engage negotiated security layer." );
            session.setAttribute( SASL_STATE, SASL_STATE_BOUND );
        }

        next.execute( session, message );
    }
}
