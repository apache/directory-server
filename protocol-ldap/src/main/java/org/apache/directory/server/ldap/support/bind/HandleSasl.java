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


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

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
public class HandleSasl implements IoHandlerCommand
{
    private static final Logger log = LoggerFactory.getLogger( HandleSasl.class );

    /**
     * A Hashed Adapter mapping SASL mechanisms to their handlers.
     */
    private static final Map DEFAULT_HANDLERS;

    static
    {
        Map<String, MechanismHandler> map = new HashMap<String, MechanismHandler>();
        map.put( "CRAM-MD5", new CramMd5MechanismHandler() );
        map.put( "DIGEST-MD5", new DigestMd5MechanismHandler() );
        map.put( "GSSAPI", new GssapiMechanismHandler() );

        DEFAULT_HANDLERS = Collections.unmodifiableMap( map );
    }


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        String sessionMechanism = ( String ) session.getAttribute( "sessionMechanism" );

        if ( DEFAULT_HANDLERS.containsKey( sessionMechanism ) )
        {
            SaslServer ss = handleMechanism( sessionMechanism, session, message );
            handleMechanism( ss, next, session, message );
        }
        else
        {
            next.execute( session, message );
        }
    }


    private SaslServer handleMechanism( String mechanism, IoSession session, Object message ) throws Exception
    {
        MechanismHandler mechanismHandler = ( MechanismHandler ) DEFAULT_HANDLERS.get( mechanism );

        if ( mechanismHandler == null )
        {
            throw new IllegalArgumentException( "Handler unavailable for " + mechanism );
        }

        return mechanismHandler.handleMechanism( session, message );
    }


    private void handleMechanism( SaslServer ss, NextCommand next, IoSession session, Object message ) throws Exception
    {
        BindRequest request = ( BindRequest ) message;
        LdapResult result = request.getResultResponse().getLdapResult();

        if ( !ss.isComplete() )
        {
            try
            {
                /*
                 * SaslServer will throw an exception if the credentials are null.
                 */
                if ( request.getCredentials() == null )
                {
                    request.setCredentials( new byte[0] );
                }

                byte[] tokenBytes = ss.evaluateResponse( request.getCredentials() );

                if ( ss.isComplete() )
                {
                    /*
                     * There may be a token to return to the client.  We set it here
                     * so it will be returned in a SUCCESS message, after an LdapContext
                     * has been initialized for the client.
                     */
                    session.setAttribute( "saslCreds", tokenBytes );

                    /*
                     * If we got here, we're ready to try getting an initial LDAP context.
                     */
                    next.execute( session, message );
                }
                else
                {
                    log.info( "Continuation token had length " + tokenBytes.length );
                    result.setResultCode( ResultCodeEnum.SASL_BIND_IN_PROGRESS );
                    BindResponse resp = ( BindResponse ) request.getResultResponse();
                    resp.setServerSaslCreds( tokenBytes );
                    session.write( resp );
                    log.debug( "Returning final authentication data to client to complete context." );
                }
            }
            catch ( SaslException se )
            {
                log.error( se.getMessage() );
                result.setResultCode( ResultCodeEnum.INVALID_CREDENTIALS );
                result.setErrorMessage( se.getMessage() );
                session.write( request.getResultResponse() );
            }
        }
    }
}
