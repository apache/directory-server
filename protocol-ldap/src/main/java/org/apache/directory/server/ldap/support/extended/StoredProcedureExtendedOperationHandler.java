/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */


package org.apache.directory.server.ldap.support.extended;


import java.nio.ByteBuffer;
import java.util.Set;

import javax.naming.ldap.Control;

import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapProtocolProvider;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedureContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedureDecoder;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.mina.common.IoSession;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class StoredProcedureExtendedOperationHandler implements ExtendedOperationHandler
{

    public String getOid()
    {
        /**
         * TODO return the correct OID. 
         */
        return "1.2.3.4.55.666.7777";
    }

    public void handleExtendedOperation( IoSession session, SessionRegistry registry, ExtendedRequest req ) throws Exception
    {
        Control[] connCtls = ( Control[] ) req.getControls().values().toArray( new Control[ req.getControls().size() ] );
        ServerLdapContext serverLdapContext = ( ServerLdapContext ) registry.getLdapContext( session, connCtls, false);
        StoredProcedure spBean = decodeBean( req.getPayload() );
        
        LanguageSpecificStoredProceureExtendedOperationHandler handler = null;
        
        /**
         * TODO This part may be replaced by a better handler determiner.
         */
        if ( spBean.getLanguage().equals( "Java" ) )
        {
            handler = new JavaStoredProcedureExtendedOperationHandler();
            handler.handleStoredProcedureExtendedOperation( serverLdapContext, spBean );
        }
    }
    
    private StoredProcedure decodeBean( byte[] payload )
    {
        Asn1Decoder storedProcedureDecoder = new StoredProcedureDecoder();
        ByteBuffer stream = ByteBuffer.wrap( payload );
        IAsn1Container storedProcedureContainer = new StoredProcedureContainer();

        try
        {
            storedProcedureDecoder.decode( stream, storedProcedureContainer );
        }
        catch (DecoderException de)
        {
            de.printStackTrace();
        }

        StoredProcedure spBean = ( ( StoredProcedureContainer ) storedProcedureContainer ).getStoredProcedure();
        
        return spBean;
    }

	public Set getExtensionOids() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setLdapProvider(LdapProtocolProvider provider) {
		// TODO Auto-generated method stub
		
	}

}
