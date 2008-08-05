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
package org.apache.directory.server.newldap.handlers.bind.gssapi;


import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.server.newldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.SaslConstants;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.BindRequest;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;
import java.security.PrivilegedExceptionAction;
import java.util.Map;


/**
 * The GSSAPI Sasl mechanism handler.
 *
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GssapiMechanismHandler implements MechanismHandler
{
    public SaslServer handleMechanism( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        SaslServer ss;

        if ( ldapSession.getIoSession().containsAttribute( SaslConstants.SASL_SERVER ) )
        {
            ss = ( SaslServer ) ldapSession.getIoSession().getAttribute( SaslConstants.SASL_SERVER );
        }
        else
        {
            Subject subject = ( Subject ) ldapSession.getIoSession().getAttribute( "saslSubject" );

            final Map<String, String> saslProps = ( Map<String, String> ) ldapSession.getIoSession().getAttribute( "saslProps" );
            final String saslHost = ( String ) ldapSession.getIoSession().getAttribute( "saslHost" );

            final CallbackHandler callbackHandler = new GssapiCallbackHandler( 
                ldapSession.getCoreSession().getDirectoryService(), ldapSession, bindRequest );

            ss = ( SaslServer ) Subject.doAs( subject, new PrivilegedExceptionAction<SaslServer>()
            {
                public SaslServer run() throws Exception
                {
                    return Sasl.createSaslServer( SupportedSaslMechanisms.GSSAPI, SaslConstants.LDAP_PROTOCOL, saslHost, saslProps, callbackHandler );
                }
            } );

            ldapSession.getIoSession().setAttribute( SaslConstants.SASL_SERVER, ss );
        }

        return ss;
    }
}
