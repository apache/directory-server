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
package org.apache.directory.server.ldap.handlers.bind.ntlm;


import javax.security.sasl.SaslServer;

import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.bind.AbstractMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SaslConstants;
import org.apache.directory.shared.ldap.model.message.BindRequest;


/**
 * A handler for the NTLM Sasl and GSS-SPNEGO mechanism. Note that both
 * mechanisms require an NTLM mechanism provider which could be implemented
 * using jCIFS or native Win32 system calls via a JNI wrapper.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NtlmMechanismHandler extends AbstractMechanismHandler
{
    private String providerFqcn;
    private NtlmProvider provider;


    public void setNtlmProvider( NtlmProvider provider )
    {
        this.provider = provider;
    }


    public void setNtlmProviderFqcn( String fqcnProvider )
    {
        this.providerFqcn = fqcnProvider;
    }


    public SaslServer handleMechanism( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        SaslServer ss = ( SaslServer ) ldapSession.getSaslProperty( SaslConstants.SASL_SERVER );

        if ( ss == null )
        {
            if ( provider == null )
            {
                initProvider();
            }
            
            ss = new NtlmSaslServer( provider, bindRequest, ldapSession, ldapSession.getLdapServer().getDirectoryService().getAdminSession() );
            ldapSession.putSaslProperty( SaslConstants.SASL_SERVER, ss );
        }

        return ss;
    }


    private void initProvider() throws Exception
    {
        provider = ( NtlmProvider ) Class.forName( providerFqcn ).newInstance();
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
     * @param ldapSession the LdapSession instance
     */
    public void cleanup( LdapSession ldapSession )
    {
        ldapSession.removeSaslProperty( SaslConstants.SASL_HOST );
        ldapSession.removeSaslProperty( SaslConstants.SASL_USER_BASE_DN );
        ldapSession.removeSaslProperty( SaslConstants.SASL_MECH );
        ldapSession.removeSaslProperty( SaslConstants.SASL_PROPS );
        ldapSession.removeSaslProperty( SaslConstants.SASL_AUTHENT_USER );
    }
}