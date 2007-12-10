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


package org.apache.directory.server.ldap.support.extended;


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.directory.Attributes;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang.SerializationUtils;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.sp.StoredProcEngine;
import org.apache.directory.server.core.sp.StoredProcEngineConfig;
import org.apache.directory.server.core.sp.StoredProcExecutionManager;
import org.apache.directory.server.core.sp.java.JavaStoredProcEngineConfig;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapProtocolProvider;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedureContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedureDecoder;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure.StoredProcedureParameter;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ExtendedResponse;
import org.apache.directory.shared.ldap.message.extended.StoredProcedureRequest;
import org.apache.directory.shared.ldap.message.extended.StoredProcedureResponse;
import org.apache.directory.shared.ldap.sp.LdapContextParameter;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.common.IoSession;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class StoredProcedureExtendedOperationHandler implements ExtendedOperationHandler
{
    private StoredProcExecutionManager manager;
    private static final Object[] EMPTY_CLASS_ARRAY = new Object[0];
    public StoredProcedureExtendedOperationHandler()
    {
        super();
        //StoredProcEngineConfig javaxScriptSPEngineConfig = new JavaxStoredProcEngineConfig();
        StoredProcEngineConfig javaSPEngineConfig = new JavaStoredProcEngineConfig();
        List<StoredProcEngineConfig> spEngineConfigs = new ArrayList<StoredProcEngineConfig>();
        //spEngineConfigs.add( javaxScriptSPEngineConfig );
        spEngineConfigs.add( javaSPEngineConfig );
        String spContainer = "ou=Stored Procedures,ou=system";
        this.manager = new StoredProcExecutionManager( spContainer, spEngineConfigs );
    }

    public void handleExtendedOperation( IoSession session, SessionRegistry registry, ExtendedRequest req ) throws Exception
    {
        Control[] connCtls = req.getControls().values().toArray( new Control[ req.getControls().size() ] );
        LdapContext ldapContext = registry.getLdapContext( session, connCtls, false);
        ServerLdapContext ctx;
        
        if ( ldapContext instanceof ServerLdapContext )
        {
            ctx = ( ServerLdapContext ) ldapContext;
        }
        else
        {
            ctx = ( ServerLdapContext ) ldapContext.lookup( "" );
        }
        
        StoredProcedure spBean = decodeBean( req.getPayload() );
        
        String procedure = StringTools.utf8ToString( spBean.getProcedure() );
        Attributes spUnit = manager.findStoredProcUnit( ctx, procedure );
        StoredProcEngine engine = manager.getStoredProcEngineInstance( spUnit );
        
        List valueList = new ArrayList( spBean.getParameters().size() );
        Iterator<StoredProcedureParameter> it = spBean.getParameters().iterator();
        while ( it.hasNext() )
        {
            StoredProcedureParameter pPojo = it.next();
            byte[] serializedValue = pPojo.getValue();
            Object value = SerializationUtils.deserialize( serializedValue );
            if ( value.getClass().equals( LdapContextParameter.class ) )
            {
                String paramCtx = ( ( LdapContextParameter ) value ).getValue();
                value = ctx.lookup( paramCtx );
            }
            valueList.add( value );
        }
        Object[] values = valueList.toArray( EMPTY_CLASS_ARRAY );
        
        Object response = engine.invokeProcedure( ctx, procedure, values );
        
        byte[] serializedResponse = SerializationUtils.serialize( ( Serializable ) response );
        ( ( ExtendedResponse )( req.getResultResponse() ) ).setResponse( serializedResponse );
        session.write( req.getResultResponse() );
        
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
        catch ( Exception de )
        {
            de.printStackTrace();
        }

        StoredProcedure spBean = ( ( StoredProcedureContainer ) storedProcedureContainer ).getStoredProcedure();
        
        return spBean;
    }

    
    public String getOid()
    {
        return StoredProcedureRequest.EXTENSION_OID;
    }


    private static final Set EXTENSION_OIDS;
    static
    {
        Set<String> s = new HashSet<String>();
        s.add( StoredProcedureRequest.EXTENSION_OID );
        s.add( StoredProcedureResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( s );
    }
    
    
    public Set getExtensionOids()
    {
        return EXTENSION_OIDS;
    }

    
	public void setLdapProvider(LdapProtocolProvider provider) 
    {
	}
}
