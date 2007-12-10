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


import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

import org.apache.mina.common.IoSession;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GssapiMechanismHandler implements MechanismHandler
{
    public SaslServer handleMechanism( IoSession session, Object message ) throws Exception
    {
        SaslServer ss;

        if ( session.containsAttribute( SASL_CONTEXT ) )
        {
            ss = ( SaslServer ) session.getAttribute( SASL_CONTEXT );
        }
        else
        {
            Subject subject = ( Subject ) session.getAttribute( "saslSubject" );

            final Map saslProps = ( Map ) session.getAttribute( "saslProps" );
            final String saslHost = ( String ) session.getAttribute( "saslHost" );

            final CallbackHandler callbackHandler = new GssapiCallbackHandler( session, message );

            ss = ( SaslServer ) Subject.doAs( subject, new PrivilegedExceptionAction()
            {
                public Object run() throws Exception
                {
                    return Sasl.createSaslServer( "GSSAPI", "ldap", saslHost, saslProps, callbackHandler );
                }
            } );

            session.setAttribute( SASL_CONTEXT, ss );
        }

        return ss;
    }
}
