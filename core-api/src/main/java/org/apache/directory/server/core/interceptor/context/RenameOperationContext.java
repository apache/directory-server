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
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;


/**
 * A RenameService context used for Interceptors. It contains all the informations
 * needed for the modify DN operation, and used by all the interceptors
 * 
 * This is used when the modifyDN is about changing the RDN, not the base DN.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RenameOperationContext extends AbstractChangeOperationContext
{
    /** The new RDN */
    private RDN newRdn;

    /** Cached copy of the new DN */
    private DN newDn;

    /** The flag to remove the old DN Attribute  */
    private boolean delOldDn;

    /** The entry after being renamed and altered for rdn attributes */ 
    private ClonedServerEntry alteredEntry;
    

    /**
     * Creates a new instance of RenameOperationContext.
     */
    public RenameOperationContext( CoreSession session )
    {
        super( session );
    }


    /**
     * Creates a new instance of RenameOperationContext.
     *
     * @param oldDn the dn of the entry before the rename
     * @param newRdn the new RDN to use for the target
     * @param delOldDn true if we delete the old RDN value
     */
    public RenameOperationContext( CoreSession session, DN oldDn, RDN newRdn, boolean delOldDn )
    {
        super( session, oldDn );
        this.newRdn = newRdn;
        this.delOldDn = delOldDn;
    }


    public RenameOperationContext( CoreSession session, InternalModifyDnRequest modifyDnRequest )
    {
        super( session, modifyDnRequest.getName() );
        this.newRdn = modifyDnRequest.getNewRdn();
        
        if ( newRdn == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_328, modifyDnRequest ) );
        }
        
        this.delOldDn = modifyDnRequest.getDeleteOldRdn();
        this.requestControls = modifyDnRequest.getControls();
        
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
     * @return The delete old DN flag
     */
    public boolean getDelOldDn() 
    {
        return delOldDn;
    }


    /**
     * Set the flag to delete the old DN
     * @param delOldDn the flag to set
     */
    public void setDelOldDn( boolean delOldDn ) 
    {
        this.delOldDn = delOldDn;
    }


    /**
     * @return The new DN either computed if null or already computed
     */
    public DN getNewDn() throws Exception
    {
        return newDn;
    }


    /**
     * @return The new RDN
     */
    public RDN getNewRdn()
    {
        return newRdn;
    }


    /**
     * Set the new RDN
     * @param newRdn The new RDN
     */
    public void setNewRdn( RDN newRdn )
    {
        this.newRdn = newRdn;
    }


    /**
     * Set the new DN
     * @param newDn The new DN
     */
    public void setNewDn( DN newDn )
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
     * Returns the entry after it has been renamed and potentially changed for 
     * RDN alterations.
     *
     * @return the new renamed entry
     */
    public ClonedServerEntry getAlteredEntry()
    {
        return alteredEntry;
    }

    
    /**
     * Set the modified entry once the operation has been proceced
     * on the backend.
     *
     * @param alteredEntry The modified entry
     */
    public void setAlteredEntry( ClonedServerEntry alteredEntry ) 
    {
        this.alteredEntry = alteredEntry;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "RenameContext for old DN '" + getDn().getName() + "'" +
        ", new RDN '" + newRdn + "'" +
        ( delOldDn ? ", delete old Dn" : "" ) ; 
    }
}
