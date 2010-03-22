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
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;


/**
 * A Move And Rename context used for Interceptors. It contains all the informations
 * needed for the modify DN operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MoveAndRenameOperationContext extends RenameOperationContext
{
    /** The parent DN */
    private DN parent;

    /** Cached calculated new DN after move and rename */
    private DN newDn;

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
    public MoveAndRenameOperationContext( CoreSession session, DN oldDn, DN parent, RDN newRdn, boolean delOldRdn )
    {
        super( session, oldDn, newRdn, delOldRdn );
        this.parent = parent;
    }


    public MoveAndRenameOperationContext( CoreSession session, InternalModifyDnRequest modifyDnRequest )
    {
        // super sets the newRdn and the delOldRdn members and tests
        super( session, modifyDnRequest );
        this.parent = modifyDnRequest.getNewSuperior();
        
        if ( parent == null )
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
    }


    /**
     *  @return The parent DN
     */
    public DN getParent()
    {
        return parent;
    }


    /**
     * Set the parent DN
     *
     * @param parent The parent
     */
    public void setParent( DN parent )
    {
        this.parent = parent;
    }

    
    /**
     * Gets cached copy of already computed new name or creates it if not 
     *
     * @return the normalized new name after move and rename
     * @throws Exception if the name cannot be normalized
     */
    public DN getNewDn() throws Exception
    {
        if ( newDn == null )
        {
            newDn = new DN( getParent().getName() );
            newDn.add( getNewRdn().getName() );
            newDn.normalize( session.getDirectoryService()
                .getSchemaManager().getNormalizerMapping() );
        }
        
        return newDn;
    }
    

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "ReplaceContext for old DN '" + getDn().getName() + "'" +
        ", parent '" + parent + "'";
    }
}
