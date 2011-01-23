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


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.security.TlsKeyGenerator;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.ldap.codec.extended.operations.certGeneration.CertGenerationContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.certGeneration.CertGenerationDecoder;
import org.apache.directory.shared.ldap.codec.extended.operations.certGeneration.CertGenerationObject;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.extended.CertGenerationRequest;
import org.apache.directory.shared.ldap.message.extended.CertGenerationResponse;
import org.apache.directory.shared.ldap.name.Dn;


/**
 * An extended handler for digital certificate generation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CertGenerationRequestHandler implements ExtendedOperationHandler
{

    private static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<String>( 2 );
        set.add( CertGenerationRequest.EXTENSION_OID );
        set.add( CertGenerationResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    public String getOid()
    {
        return CertGenerationRequest.EXTENSION_OID;
    }


    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    public void handleExtendedOperation( LdapSession session, ExtendedRequest req ) throws Exception
    {
        ByteBuffer bb = ByteBuffer.wrap( req.getRequestValue() );
        Asn1Decoder decoder = new CertGenerationDecoder();
        CertGenerationContainer container = new CertGenerationContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException e )
        {
            throw e;
        }

        CertGenerationObject certGenObj = container.getCertGenerationObject();

        Entry entry = session.getCoreSession().lookup( new Dn( certGenObj.getTargetDN() ) );

        if ( entry != null )
        {
            TlsKeyGenerator.addKeyPair( ( ( ClonedServerEntry ) entry ).getOriginalEntry(), certGenObj.getIssuerDN(),
                certGenObj.getSubjectDN(), certGenObj.getKeyAlgorithm() );
        }
    }


    public void setLdapServer( LdapServer ldapServer )
    {
    }

}
