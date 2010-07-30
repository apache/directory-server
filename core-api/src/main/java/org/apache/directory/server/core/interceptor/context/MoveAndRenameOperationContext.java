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
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;


/**
 * A Move And Rename context used for Interceptors. It contains all the informations
 * needed for the modify DN operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MoveAndRenameOperationContext extends RenameOperationContext
{
    /** The new superior DN */
    private DN newSuperiorDn;

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
     * @param oldDn the original source entry DN to be moved and renamed
     * @param parent the new entry superior of the target after the move
     * @param newRdn the new rdn to use for the target once renamed
     * @param delOldRdn true if the old rdn value is deleted, false otherwise
     */
    public MoveAndRenameOperationContext( CoreSession session, DN oldDn, DN newSuperiorDn, RDN newRdn, boolean delOldRdn )
    {
        super( session, oldDn, newRdn, delOldRdn );
        this.newSuperiorDn = newSuperiorDn;
        newDn = newSuperiorDn.add( newRdn );
    }


    public MoveAndRenameOperationContext( CoreSession session, InternalModifyDnRequest modifyDnRequest )
    {
        // super sets the newRdn and the delOldRdn members and tests
        super( session, modifyDnRequest );
        this.newSuperiorDn = modifyDnRequest.getNewSuperior();
        
        if ( newSuperiorDn == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_325, modifyDnRequest ) );
        }
        
        if ( requestControls.containsKey( ManageDsaITControl.CONTROL_OID ) )
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
                .getSchemaManager().getNormalizerMapping() );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_325, modifyDnRequest ) );
        }
    }


    /**
     *  @return The new superior DN
     */
    public DN getNewSuperiorDn()
    {
        return newSuperiorDn;
    }


    /**
     * Set the new Superior DN
     *
     * @param newSuperiorDn The new Superior DN
     */
    public void setNewSuperiorDn( DN newSuperiorDn )
    {
        this.newSuperiorDn = newSuperiorDn;
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "ReplaceContext for old DN '" + getDn().getName() + "' : " + newDn;
    }
}
