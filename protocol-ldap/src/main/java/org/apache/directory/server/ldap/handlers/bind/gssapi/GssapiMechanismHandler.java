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
package org.apache.directory.server.ldap.handlers.bind.gssapi;


import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.bind.AbstractMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SaslConstants;
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
public class GssapiMechanismHandler extends AbstractMechanismHandler
{
    public SaslServer handleMechanism( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        SaslServer ss = (SaslServer)ldapSession.getSaslProperty( SaslConstants.SASL_SERVER );

        if ( ss == null )
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

    
    /**
     * {@inheritDoc}
     */
    public void init( LdapSession ldapSession )
    {
        // Store the host in the ldap session
        String saslHost = ldapSession.getLdapServer().getSaslHost();
        ldapSession.putSaslProperty( SaslConstants.SASL_HOST, saslHost );
    }


    /**
     * Remove the Host, UserBaseDn, props and Mechanism property.
     * 
     * @param ldapSession the Ldapsession instance
     */
    public void cleanup( LdapSession ldapSession )
    {
        // Inject the Sasl Filter
        insertSaslFilter( ldapSession );

        // and remove the useless informations
        ldapSession.removeSaslProperty( SaslConstants.SASL_HOST );
        ldapSession.removeSaslProperty( SaslConstants.SASL_USER_BASE_DN );
        ldapSession.removeSaslProperty( SaslConstants.SASL_MECH );
        ldapSession.removeSaslProperty( SaslConstants.SASL_PROPS );
        ldapSession.removeSaslProperty( SaslConstants.SASL_AUTHENT_USER );
    }
}
