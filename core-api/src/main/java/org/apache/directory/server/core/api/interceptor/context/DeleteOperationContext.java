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


import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A Delete context used for Interceptors. It contains all the informations
 * needed for the delete operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DeleteOperationContext extends AbstractChangeOperationContext
{
    /**
     * Creates a new instance of DeleteOperationContext.
     */
    public DeleteOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.DELETE ) );
        }
    }


    /**
     * Creates a new instance of DeleteOperationContext.
     *
     * @param deleteDn The entry Dn to delete
     */
    public DeleteOperationContext( CoreSession session, Dn deleteDn )
    {
        super( session, deleteDn );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.DELETE ) );
        }
    }


    public DeleteOperationContext( CoreSession session, DeleteRequest deleteRequest )
    {
        super( session, deleteRequest.getName() );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.DELETE ) );
        }

        requestControls = deleteRequest.getControls();

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
     * {@inheritDoc}
     */
    public void resetContext()
    {
        super.resetContext();
        entry = null;
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.DEL_REQUEST.name();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "DeleteContext for Dn '" + getDn().getName() + "'";
    }


    /**
     * @param entry the entry to set
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }
}
