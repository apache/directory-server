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
package org.apache.directory.server.ldap.handlers.extended;


import java.util.Collections; 
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.security.TlsKeyGenerator;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.extras.extended.CertGenerationRequest;
import org.apache.directory.shared.ldap.extras.extended.CertGenerationResponse;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * An extended handler for digital certificate generation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CertGenerationRequestHandler 
    implements ExtendedOperationHandler<CertGenerationRequest, CertGenerationResponse>
{
    private static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<String>( 2 );
        set.add( CertGenerationRequest.EXTENSION_OID );
        set.add( CertGenerationResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    /**
     * {@inheritDoc}
     */
    public String getOid()
    {
        return CertGenerationRequest.EXTENSION_OID;
    }


    /**
     * {@inheritDoc}
     */
    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    /**
     * {@inheritDoc}
     */
    public void handleExtendedOperation( LdapSession session, CertGenerationRequest req ) throws Exception
    {
        Entry entry = session.getCoreSession().lookup( new Dn( req.getTargetDN() ) );

        if ( entry != null )
        {
            TlsKeyGenerator.addKeyPair( 
                ( ( ClonedServerEntry ) entry ).getOriginalEntry(), 
                req.getIssuerDN(),
                req.getSubjectDN(), 
                req.getKeyAlgorithm() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void setLdapServer( LdapServer ldapServer )
    {
    }
}
