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
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.control.ManageDsaITControl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;


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
    private Rdn newRdn;

    /** Cached copy of the new DN */
    private LdapDN newDn;

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
    public RenameOperationContext( CoreSession session, LdapDN oldDn, Rdn newRdn, boolean delOldDn )
    {
        super( session, oldDn );
        this.newRdn = newRdn;
        this.delOldDn = delOldDn;
    }


    public RenameOperationContext( CoreSession session, ModifyDnRequest modifyDnRequest )
    {
        super( session, modifyDnRequest.getName() );
        this.newRdn = modifyDnRequest.getNewRdn();
        
        if ( newRdn == null )
        {
            throw new IllegalStateException( "newRdn must not be null for a rename: " + modifyDnRequest );
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
    public LdapDN getNewDn() throws Exception
    {
        if ( newDn == null )
        {
            newDn = new LdapDN( getDn().getUpName() );
            newDn.remove( newDn.size() - 1 );
            newDn.add( newRdn.getUpName() );
            newDn.normalize( session.getDirectoryService().getRegistries()
                .getAttributeTypeRegistry().getNormalizerMapping() );
        }
        
        return newDn;
    }


    /**
     * @return The new RDN
     */
    public Rdn getNewRdn()
    {
        return newRdn;
    }


    /**
     * Set the new RDN
     * @param newRdn The new RDN
     */
    public void setNewRdn( Rdn newRdn )
    {
        this.newRdn = newRdn;
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.MOD_DN_REQUEST.name();
    }
    
    
    /**
     * Returns the entry after it has been renamed and potentially changed for 
     * Rdn alterations.
     *
     * @return the new renamed entry
     */
    public ClonedServerEntry getAlteredEntry()
    {
        return alteredEntry;
    }

    
    public void setAlteredEntry( ClonedServerEntry alteredEntry ) 
    {
        this.alteredEntry = alteredEntry;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "RenameContext for old DN '" + getDn().getUpName() + "'" +
        ", new RDN '" + newRdn + "'" +
        ( delOldDn ? ", delete old Dn" : "" ) ; 
    }
}
