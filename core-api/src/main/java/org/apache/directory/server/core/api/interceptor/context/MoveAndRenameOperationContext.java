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


import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.i18n.I18n;


/**
 * A Move And Rename context used for Interceptors. It contains all the informations
 * needed for the modify Dn operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MoveAndRenameOperationContext extends RenameOperationContext
{
    /** The new superior Dn */
    private Dn newSuperiorDn;

    /** The map of modified AVAs */
    private Map<String, List<ModDnAva>> modifiedAvas;

    /**
     * Creates a new instance of MoveAndRenameOperationContext.
     * 
     * @param session The session to use
     */
    public MoveAndRenameOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MOVE_AND_RENAME ) );
        }
    }


    /**
     * Creates a new instance of MoveAndRenameOperationContext.
     *
     * @param session The session to use
     * @param oldDn the original source entry Dn to be moved and renamed
     * @param newSuperiorDn the new entry superior of the target after the move
     * @param newRdn the new rdn to use for the target once renamed
     * @param delOldRdn true if the old rdn value is deleted, false otherwise
     */
    public MoveAndRenameOperationContext( CoreSession session, Dn oldDn, Dn newSuperiorDn, Rdn newRdn, boolean delOldRdn )
    {
        super( session, oldDn, newRdn, delOldRdn );
        this.newSuperiorDn = newSuperiorDn;

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MOVE_AND_RENAME ) );
        }

        try
        {
            newDn = newSuperiorDn.add( newRdn );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new IllegalArgumentException( lide.getMessage(), lide );
        }
    }


    public MoveAndRenameOperationContext( CoreSession session, ModifyDnRequest modifyDnRequest )
    {
        // super sets the newRdn and the delOldRdn members and tests
        super( session, modifyDnRequest );
        newSuperiorDn = modifyDnRequest.getNewSuperior();
        
        if ( !newSuperiorDn.isSchemaAware() )
        {
            try
            {
                newSuperiorDn = new Dn( session.getDirectoryService().getSchemaManager(), newSuperiorDn );
            }
            catch ( LdapInvalidDnException lide )
            {
                throw new IllegalStateException( I18n.err( I18n.ERR_02018_NEW_SUPERIOR_MUST_BE_NOT_NULL, modifyDnRequest ), lide );
            }
        }

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MOVE_AND_RENAME ) );
        }

        if ( newSuperiorDn == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_02018_NEW_SUPERIOR_MUST_BE_NOT_NULL, modifyDnRequest ) );
        }

        if ( requestControls.containsKey( ManageDsaIT.OID ) )
        {
            ignoreReferral();
        }
        else
        {
            throwReferral();
        }

        try
        {
            newDn = newSuperiorDn.add( newRdn );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_02018_NEW_SUPERIOR_MUST_BE_NOT_NULL, modifyDnRequest ), lide );
        }
    }


    /**
     *  @return The new superior Dn
     */
    public Dn getNewSuperiorDn()
    {
        return newSuperiorDn;
    }


    /**
     * Set the new Superior Dn
     *
     * @param newSuperiorDn The new Superior Dn
     */
    public void setNewSuperiorDn( Dn newSuperiorDn )
    {
        this.newSuperiorDn = newSuperiorDn;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return "ReplaceContext for old Dn '" + getDn().getName() + "' : " + newDn;
    }


    /**
     * @return the modifiedAvas
     */
    public Map<String, List<ModDnAva>> getModifiedAvas()
    {
        return modifiedAvas;
    }


    /**
     * @param modifiedAvas the modifiedAvas to set
     */
    public void setModifiedAvas( Map<String, List<ModDnAva>> modifiedAvas )
    {
        this.modifiedAvas = modifiedAvas;
    }
}
