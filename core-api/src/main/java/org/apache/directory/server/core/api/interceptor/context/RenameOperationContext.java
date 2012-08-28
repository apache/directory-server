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
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;


/**
 * A RenameService context used for Interceptors. It contains all the informations
 * needed for the modify Dn operation, and used by all the interceptors
 * 
 * This is used when the modifyDN is about changing the Rdn, not the base Dn.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RenameOperationContext extends AbstractChangeOperationContext
{
    /** The new Rdn */
    protected Rdn newRdn;

    /** Cached copy of the new Dn */
    protected Dn newDn;

    /** The flag to remove the old Rdn Attribute  */
    private boolean deleteOldRdn;


    /**
     * Creates a new instance of RenameOperationContext.
     */
    public RenameOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.RENAME ) );
        }
    }


    /**
     * Creates a new instance of RenameOperationContext.
     *
     * @param oldDn the dn of the entry before the rename
     * @param newRdn the new Rdn to use for the target
     * @param delOldDn true if we delete the old Rdn value
     */
    public RenameOperationContext( CoreSession session, Dn oldDn, Rdn newRdn, boolean deleteOldRdn )
    {
        super( session, oldDn );
        this.newRdn = newRdn;
        this.deleteOldRdn = deleteOldRdn;

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.RENAME ) );
        }
    }


    public RenameOperationContext( CoreSession session, ModifyDnRequest modifyDnRequest )
    {
        super( session, modifyDnRequest.getName() );
        this.newRdn = modifyDnRequest.getNewRdn();

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.RENAME ) );
        }

        if ( newRdn == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_328, modifyDnRequest ) );
        }

        this.deleteOldRdn = modifyDnRequest.getDeleteOldRdn();
        this.requestControls = modifyDnRequest.getControls();

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
     * @return The delete old Rdn flag
     */
    public boolean getDeleteOldRdn()
    {
        return deleteOldRdn;
    }


    /**
     * Set the flag to delete the old Rdn
     * @param deleteOldRdn the flag to set
     */
    public void setDelOldDn( boolean deleteOldRdn )
    {
        this.deleteOldRdn = deleteOldRdn;
    }


    /**
     * @return The new Dn either computed if null or already computed
     */
    public Dn getNewDn()
    {
        return newDn;
    }


    /**
     * @return The new Rdn
     */
    public Rdn getNewRdn()
    {
        return newRdn;
    }


    /**
     * Set the new Rdn
     * @param newRdn The new Rdn
     */
    public void setNewRdn( Rdn newRdn )
    {
        this.newRdn = newRdn;
    }


    /**
     * Set the new Dn
     * @param newDn The new Dn
     */
    public void setNewDn( Dn newDn )
    {
        this.newDn = newDn;
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.MODIFYDN_REQUEST.name();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "RenameContext for old Dn '" + getDn().getName() + "'" +
            ", new Rdn '" + newRdn + "'" +
            ( deleteOldRdn ? ", delete old Rdn" : "" );
    }
}
