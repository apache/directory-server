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


import javax.security.sasl.SaslServer;

import org.apache.directory.shared.ldap.constants.SupportedSASLMechanisms;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.BindResponse;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link IoHandlerCommand} for finalizing a successful bind.  A successful bind
 * will require both authentication and LDAP context acquisition.  If the LDAP client
 * is both authenticated and able to acquire an LDAP context, an LDAP SUCCESS message
 * is returned.  If the authentication mechanism was either DIGEST-MD5 or GSSAPI, a
 * {@link SaslFilter} is constructed with the initialized {@link SaslServer} context
 * and the {@link SaslFilter} is inserted into the {@link IoFilterChain} for this
 * instance of the LDAP protocol.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ReturnSuccess implements IoHandlerCommand
{
    private static final Logger log = LoggerFactory.getLogger( ReturnSuccess.class );

    private static final String SASL_CONTEXT = "saslContext";


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

        String sessionMechanism = ( String ) session.getAttribute( "sessionMechanism" );

        /*
         * If the SASL mechanism is DIGEST-MD5 or GSSAPI, we insert a SASLFilter.
         */
        if ( sessionMechanism.equals( SupportedSASLMechanisms.DIGEST_MD5 ) || 
             sessionMechanism.equals( SupportedSASLMechanisms.GSSAPI ) )
        {
            log.debug( "Inserting SaslFilter to engage negotiated security layer." );

            IoFilterChain chain = session.getFilterChain();
            if ( !chain.contains( "SASL" ) )
            {
                SaslServer saslContext = ( SaslServer ) session.getAttribute( SASL_CONTEXT );
                chain.addBefore( "codec", "SASL", new SaslFilter( saslContext ) );
            }

            /*
             * We disable the SASL security layer once, to write the outbound SUCCESS
             * message without SASL security layer processing.
             */
            session.setAttribute( SaslFilter.DISABLE_SECURITY_LAYER_ONCE, Boolean.TRUE );
        }

        session.write( response );
        log.debug( "Returned SUCCESS message." );

        next.execute( session, message );
    }
}
