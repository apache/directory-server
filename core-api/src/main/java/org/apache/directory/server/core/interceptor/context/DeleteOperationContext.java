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
package org.apache.directory.server.core.interceptor.context;


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.name.Dn;


/**
 * A Delete context used for Interceptors. It contains all the informations
 * needed for the delete operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DeleteOperationContext extends AbstractChangeOperationContext
{
    /**
     * An optimization added to prevent redundant lookups of the deleted 
     * entry.
     */
    private ClonedServerEntry entry;
    
    
    /**
     * Creates a new instance of DeleteOperationContext.
     */
    public DeleteOperationContext( CoreSession session )
    {
        super( session );
    }
    

    /**
     * Creates a new instance of DeleteOperationContext.
     *
     * @param deleteDn The entry Dn to delete
     */
    public DeleteOperationContext( CoreSession session, Dn deleteDn )
    {
        super( session, deleteDn );
    }


    public DeleteOperationContext( CoreSession session, DeleteRequest deleteRequest )
    {
        super( session, deleteRequest.getName() );
        requestControls = deleteRequest.getControls();
        
        if ( requestControls.containsKey( ManageDsaITControl.CONTROL_OID ) )
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
    public void setEntry( ClonedServerEntry entry )
    {
        this.entry = entry;
    }


    /**
     * Gets the deleted entry if cached.  Must be called before deleting the 
     * entry when the entry member is null or this call will fail.  
     * 
     * @return the entry
     */
    public ClonedServerEntry getEntry()
    {
        return entry;
    }
}
