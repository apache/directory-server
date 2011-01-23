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


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.sp.StoredProcEngine;
import org.apache.directory.server.core.sp.StoredProcEngineConfig;
import org.apache.directory.server.core.sp.StoredProcExecutionManager;
import org.apache.directory.server.core.sp.java.JavaStoredProcEngineConfig;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure.StoredProcedure;
import org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure.StoredProcedureContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure.StoredProcedureDecoder;
import org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure.StoredProcedure.StoredProcedureParameter;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ExtendedResponse;
import org.apache.directory.shared.ldap.message.extended.StoredProcedureRequest;
import org.apache.directory.shared.ldap.message.extended.StoredProcedureResponse;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.sp.LdapContextParameter;
import org.apache.directory.shared.util.Strings;


/**
 * @todo : Missing Javadoc
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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


    public void handleExtendedOperation( LdapSession session, ExtendedRequest req ) throws Exception
    {
        StoredProcedure spBean = decodeBean( req.getRequestValue() );

        String procedure = Strings.utf8ToString(spBean.getProcedure());
        ClonedServerEntry spUnit = manager.findStoredProcUnit( session.getCoreSession(), procedure );
        StoredProcEngine engine = manager.getStoredProcEngineInstance( spUnit );

        List<Object> valueList = new ArrayList<Object>( spBean.getParameters().size() );

        for ( StoredProcedureParameter pPojo : spBean.getParameters() )
        {
            byte[] serializedValue = pPojo.getValue();
            Object value = SerializationUtils.deserialize( serializedValue );

            if ( value.getClass().equals( LdapContextParameter.class ) )
            {
                String paramCtx = ( ( LdapContextParameter ) value ).getValue();
                value = session.getCoreSession().lookup( new Dn( paramCtx ) );
            }

            valueList.add( value );
        }

        Object[] values = valueList.toArray( EMPTY_CLASS_ARRAY );

        Object response = engine.invokeProcedure( session.getCoreSession(), procedure, values );

        byte[] serializedResponse = SerializationUtils.serialize( ( Serializable ) response );
        ( ( ExtendedResponse ) ( req.getResultResponse() ) ).setResponseValue( serializedResponse );
        session.getIoSession().write( req.getResultResponse() );

    }


    private StoredProcedure decodeBean( byte[] payload )
    {
        Asn1Decoder storedProcedureDecoder = new StoredProcedureDecoder();
        ByteBuffer stream = ByteBuffer.wrap( payload );
        Asn1Container storedProcedureContainer = new StoredProcedureContainer();

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

    private static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> s = new HashSet<String>();
        s.add( StoredProcedureRequest.EXTENSION_OID );
        s.add( StoredProcedureResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( s );
    }


    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    public void setLdapServer( LdapServer ldapServer )
    {
    }
}
