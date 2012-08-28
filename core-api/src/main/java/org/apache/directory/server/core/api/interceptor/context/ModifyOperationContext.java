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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.changelog.LogChange;
import org.apache.directory.server.core.api.entry.ServerEntryUtils;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A Modify context used for Interceptors. It contains all the informations
 * needed for the modify operation, and used by all the interceptors
 * 
 * This context can use either Attributes or ModificationItem, but not both.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyOperationContext extends AbstractChangeOperationContext
{
    /** The modification items */
    private List<Modification> modItems;

    /** Orignal list of modification items */
    private List<Modification> originalModItems = new ArrayList<Modification>();

    /** The entry after being renamed and altered for rdn attributes */
    private Entry alteredEntry;


    /**
     * Creates a new instance of ModifyOperationContext.
     */
    public ModifyOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MODIFY ) );
        }
    }


    /**
     * Creates a new instance of ModifyOperationContext.
     *
     * @param dn the dn of the entry to be modified
     * @param modItems the modifications to be performed on the entry
     */
    public ModifyOperationContext( CoreSession session, Dn dn, List<Modification> modItems )
    {
        super( session, dn );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MODIFY ) );
        }

        this.modItems = modItems;
    }


    public ModifyOperationContext( CoreSession session, ModifyRequest modifyRequest ) throws LdapException
    {
        super( session, modifyRequest.getName() );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MODIFY ) );
        }

        modItems = ServerEntryUtils.toServerModification( modifyRequest.getModifications().toArray(
            new DefaultModification[0] ), session.getDirectoryService().getSchemaManager() );

        requestControls = modifyRequest.getControls();

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
    public void saveOriginalContext()
    {
        super.saveOriginalContext();

        List<Modification> items = getModItems();

        if ( items != null )
        {
            for ( Modification mod : items )
            {
                originalModItems.add( mod.clone() );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void resetContext()
    {
        super.resetContext();
        alteredEntry = null;

        if ( modItems != null )
        {
            modItems.clear();
            modItems.addAll( originalModItems );
        }

        originalModItems.clear();
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


    public static List<Modification> createModItems( Entry serverEntry, ModificationOperation modOp )
        throws LdapException
    {
        List<Modification> items = new ArrayList<Modification>( serverEntry.size() );

        for ( Attribute attribute : serverEntry )
        {
            items.add( new DefaultModification( modOp, attribute ) );
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


    /**
     * Returns the entry after it has been renamed and potentially changed for
     * Rdn alterations.
     *
     * @return the new renamed entry
     */
    public Entry getAlteredEntry()
    {
        return alteredEntry;
    }


    /**
     * Set the modified entry once the operation has been proceced
     * on the backend.
     *
     * @param alteredEntry The modified entry
     */
    public void setAlteredEntry( Entry alteredEntry )
    {
        this.alteredEntry = alteredEntry;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ModifyContext for Dn '" ).append( getDn().getName() ).append( "', modifications :\n" );

        if ( modItems != null )
        {
            for ( Modification mod : modItems )
            {
                sb.append( mod ).append( '\n' );
            }
        }

        return sb.toString();
    }
}
