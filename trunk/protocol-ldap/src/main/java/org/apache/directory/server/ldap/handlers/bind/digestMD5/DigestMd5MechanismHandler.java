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
package org.apache.directory.server.ldap.handlers.bind.digestMD5;


import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.bind.AbstractMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SaslConstants;
import org.apache.directory.shared.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.model.message.BindRequest;


/**
 * The DIGEST-MD5 mechanism handler.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DigestMd5MechanismHandler extends AbstractMechanismHandler
{
    /**
     * Create a list of all the configured realms.
     * 
     * @param ldapServer the LdapServer for which we want to get the realms
     * @return a list of realms, separated by spaces
     */
    private String getActiveRealms( LdapServer ldapServer )
    {
        StringBuilder realms = new StringBuilder();
        boolean isFirst = true;

        for ( String realm : ldapServer.getSaslRealms() )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                realms.append( ' ' );
            }

            realms.append( realm );
        }

        return realms.toString();
    }


    public SaslServer handleMechanism( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        SaslServer ss = ( SaslServer ) ldapSession.getSaslProperty( SaslConstants.SASL_SERVER );

        if ( ss == null )
        {
            CoreSession adminSession = ldapSession.getLdapServer().getDirectoryService().getAdminSession();

            CallbackHandler callbackHandler = new DigestMd5CallbackHandler( ldapSession, adminSession, bindRequest );

            ss = Sasl.createSaslServer(
                SupportedSaslMechanisms.DIGEST_MD5,
                SaslConstants.LDAP_PROTOCOL,
                ( String ) ldapSession.getSaslProperty( SaslConstants.SASL_HOST ),
                ( Map<String, String> ) ldapSession.getSaslProperty( SaslConstants.SASL_PROPS ),
                callbackHandler );
            ldapSession.putSaslProperty( SaslConstants.SASL_SERVER, ss );
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
        String userBaseDn = ldapSession.getLdapServer().getSearchBaseDn();

        ldapSession.putSaslProperty( SaslConstants.SASL_HOST, saslHost );
        ldapSession.putSaslProperty( SaslConstants.SASL_USER_BASE_DN, userBaseDn );

        Map<String, String> saslProps = new HashMap<String, String>();
        saslProps.put( Sasl.QOP, ldapSession.getLdapServer().getSaslQopString() );
        saslProps.put( "com.sun.security.sasl.digest.realm", getActiveRealms( ldapSession.getLdapServer() ) );
        ldapSession.putSaslProperty( SaslConstants.SASL_PROPS, saslProps );
    }


    /**
     * Remove the Host, UserBaseDn, props and Mechanism property.
     * 
     * @param ldapSession the LdapSession instance
     */
    public void cleanup( LdapSession ldapSession )
    {
        // Inject the Sasl Filter
        insertSaslFilter( ldapSession );

        // and cleanup the useless informations
        ldapSession.removeSaslProperty( SaslConstants.SASL_HOST );
        ldapSession.removeSaslProperty( SaslConstants.SASL_USER_BASE_DN );
        ldapSession.removeSaslProperty( SaslConstants.SASL_MECH );
        ldapSession.removeSaslProperty( SaslConstants.SASL_PROPS );
        ldapSession.removeSaslProperty( SaslConstants.SASL_AUTHENT_USER );
    }
}
