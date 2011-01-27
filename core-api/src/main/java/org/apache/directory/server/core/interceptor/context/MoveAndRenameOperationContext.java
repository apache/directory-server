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
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;


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

    /**
     * Creates a new instance of MoveAndRenameOperationContext.
     */
    public MoveAndRenameOperationContext( CoreSession session )
    {
        super( session );
    }


    /**
     * Creates a new instance of MoveAndRenameOperationContext.
     *
     * @param oldDn the original source entry Dn to be moved and renamed
     * @param parent the new entry superior of the target after the move
     * @param newRdn the new rdn to use for the target once renamed
     * @param delOldRdn true if the old rdn value is deleted, false otherwise
     */
    public MoveAndRenameOperationContext( CoreSession session, Dn oldDn, Dn newSuperiorDn, Rdn newRdn, boolean delOldRdn )
    {
        super( session, oldDn, newRdn, delOldRdn );
        this.newSuperiorDn = newSuperiorDn;
        newDn = newSuperiorDn.add( newRdn );
    }


    public MoveAndRenameOperationContext( CoreSession session, ModifyDnRequest modifyDnRequest )
    {
        // super sets the newRdn and the delOldRdn members and tests
        super( session, modifyDnRequest );
        this.newSuperiorDn = modifyDnRequest.getNewSuperior();

        if ( newSuperiorDn == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_325, modifyDnRequest ) );
        }

        if ( requestControls.containsKey( ManageDsaIT.OID ) )
        {
            ignoreReferral();
        }
        else
        {
            throwReferral();
        }

        newDn = newSuperiorDn.add(newRdn);

        try
        {
            newDn.normalize( session.getDirectoryService()
                .getSchemaManager() );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_325, modifyDnRequest ) );
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
    public String toString()
    {
        return "ReplaceContext for old Dn '" + getDn().getName() + "' : " + newDn;
    }
}
