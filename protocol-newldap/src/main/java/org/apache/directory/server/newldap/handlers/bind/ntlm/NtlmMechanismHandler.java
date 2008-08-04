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
package org.apache.directory.server.newldap.handlers.bind.ntlm;


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.server.newldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.SaslConstants;
import org.apache.directory.shared.ldap.message.BindRequest;

import javax.security.sasl.SaslServer;


/**
 * A handler for the NTLM Sasl and GSS-SPNEGO mechanism. Note that both
 * mechanisms require an NTLM mechanism provider which could be implemented
 * using jCIFS or native Win32 system calls via a JNI wrapper.
 *
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtlmMechanismHandler implements MechanismHandler
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


    public SaslServer handleMechanism( LdapSession ldapSession, CoreSession adminSession, BindRequest bindRequest ) throws Exception
    {
        SaslServer ss = ( SaslServer ) ldapSession.getSaslProperty( SaslConstants.SASL_SERVER );

        if ( ss == null )
        {
            if ( provider == null )
            {
                initProvider();
            }
            
            ss = new NtlmSaslServer( provider, bindRequest, ldapSession );
            ldapSession.putSaslProperty( SaslConstants.SASL_SERVER, ss );
        }

        return ss;
    }


    private void initProvider() throws Exception
    {
        provider = ( NtlmProvider ) Class.forName( providerFqcn ).newInstance();
    }
}