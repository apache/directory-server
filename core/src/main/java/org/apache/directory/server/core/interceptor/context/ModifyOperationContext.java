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


import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.entry.ServerModification;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.control.ManageDsaITControl;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A Modify context used for Interceptors. It contains all the informations
 * needed for the modify operation, and used by all the interceptors
 * 
 * This context can use either Attributes or ModificationItem, but not both.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ModifyOperationContext extends AbstractChangeOperationContext
{
    /** The modification items */
    private List<Modification> modItems;
    
    /** Cloned entry that is modified */
    private ClonedServerEntry entry;


    /**
     * Creates a new instance of ModifyOperationContext.
     */
    public ModifyOperationContext( CoreSession session )
    {
        super( session );
    }


    /**
     * Creates a new instance of ModifyOperationContext.
     *
     * @param dn the dn of the entry to be modified
     * @param modItems the modifications to be performed on the entry
     */
    public ModifyOperationContext( CoreSession session, LdapDN dn, List<Modification> modItems )
    {
        super( session, dn );

        this.modItems = modItems;
    }


    public ModifyOperationContext( CoreSession session, ModifyRequest modifyRequest ) throws Exception
    {
        super( session, modifyRequest.getName() );
        
        modItems = ServerEntryUtils.toServerModification( 
            modifyRequest.getModificationItems().toArray( new ClientModification[0]), 
            session.getDirectoryService().getRegistries().getAttributeTypeRegistry() );
        
        requestControls = modifyRequest.getControls();

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
     * Set the modified attributes
     * @param modItems The modified attributes
     */
    public void setModItems( List<Modification> modItems )
    {
        this.modItems = modItems;
    }


    /**
     * @return The modifications
     */
    public List<Modification> getModItems() 
    {
        return modItems;
    }


    public static List<Modification> createModItems( ServerEntry serverEntry, ModificationOperation modOp ) throws NamingException
    {
        List<Modification> items = new ArrayList<Modification>( serverEntry.size() );
        
        for ( EntryAttribute attribute:serverEntry )
        {
            items.add( new ServerModification( modOp, attribute ) );
        }

        return items;
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.MODIFY_REQUEST.name();
    }

    
    public void setEntry( ClonedServerEntry entry )
    {
        this.entry = entry;
    }
    
    
    public ClonedServerEntry getEntry()
    {
        return entry;
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("ModifyContext for DN '").append( getDn().getUpName() ).append( "', modifications :\n" );
        
        for ( Modification mod:modItems )
        {
            sb.append( mod ).append( '\n' );
        }
        
        return sb.toString();
    }
}
