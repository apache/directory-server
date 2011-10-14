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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.apache.directory.server.core.shared.sp.StoredProcEngine;
import org.apache.directory.server.core.shared.sp.StoredProcEngineConfig;
import org.apache.directory.server.core.shared.sp.StoredProcExecutionManager;
import org.apache.directory.server.core.shared.sp.java.JavaStoredProcEngineConfig;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.shared.ldap.extras.extended.StoredProcedureRequest;
import org.apache.directory.shared.ldap.extras.extended.StoredProcedureResponse;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.sp.LdapContextParameter;


/**
 * @todo : Missing Javadoc
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureExtendedOperationHandler implements ExtendedOperationHandler<StoredProcedureRequest, StoredProcedureResponse>
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


    public void handleExtendedOperation( LdapSession session, StoredProcedureRequest req ) throws Exception
    {
        String procedure = req.getProcedureSpecification();
        Entry spUnit = manager.findStoredProcUnit( session.getCoreSession(), procedure );
        StoredProcEngine engine = manager.getStoredProcEngineInstance( spUnit );

        List<Object> valueList = new ArrayList<Object>( req.size() );
        
        for ( int ii = 0; ii < req.size(); ii++ )
        {
            byte[] serializedValue = ( byte[] ) req.getParameterValue( ii );
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
        StoredProcedureResponse resp = 
            LdapApiServiceFactory.getSingleton().newExtendedResponse( req, serializedResponse );
        session.getIoSession().write( resp );
    }

    
    /**
     * {@inheritDoc}
     */
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
    public void setLdapServer( LdapServer ldapServer )
    {
    }
}
