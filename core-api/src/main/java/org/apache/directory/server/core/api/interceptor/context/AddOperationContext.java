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
package org.apache.directory.server.core.api.interceptor.context;


import java.util.Collections;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A Add context used for Interceptors. It contains all the informations
 * needed for the add operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddOperationContext extends AbstractChangeOperationContext
{
    /**
     * Creates a new instance of AddOperationContext.
     * 
     * @param session the current Session 
     */
    public AddOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.ADD ) );
        }
    }


    /**
     * Creates a new instance of AddOperationContext.
     * 
     * @param session the current Session 
     * @param dn the name of the entry being added
     */
    public AddOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.ADD ) );
        }
    }


    /**
     * Creates a new instance of AddOperationContext.
     * 
     * @param session the current Session 
     * @param entry the entry being added
     */
    public AddOperationContext( CoreSession session, Entry entry )
    {
        super( session, entry.getDn() );
        this.entry = new ClonedServerEntry( entry );
        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.ADD ) );
        }
    }


    /**
     * Creates a new instance of ModifyOperationContext.
     *
     * @param session the current Session 
     * @param dn the name of the entry being added
     * @param entry the entry being added
     */
    public AddOperationContext( CoreSession session, Dn dn, Entry entry )
    {
        super( session, dn );
        
        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.ADD ) );
        }
        
        this.entry = new ClonedServerEntry( entry );
    }


    public AddOperationContext( CoreSession session, AddRequest addRequest ) throws LdapException
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.ADD ) );
        }
        
        entry = new ClonedServerEntry( 
            new DefaultEntry( session.getDirectoryService().getSchemaManager(), addRequest.getEntry() ) );
        dn = addRequest.getEntry().getDn();
        requestControls = addRequest.getControls();
        
        if ( requestControls.containsKey( ManageDsaIT.OID ) )
        {
            ignoreReferral();
        }
        else
        {
            throwReferral();
        }
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.ADD_REQUEST.name();
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "AddContext for Dn '" + getDn().getName() + "'" + ", added entry: " + entry;
    }
}
