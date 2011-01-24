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
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.model.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;


/**
 * A Move context used for Interceptors. It contains all the informations
 * needed for the modify Dn operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MoveOperationContext extends AbstractChangeOperationContext
{
    /** The old superior */
    private Dn oldSuperior;

    /** The entry Rdn */
    private Rdn rdn;
    
    /** The newSuperior Dn */
    private Dn newSuperior;
    
    /** The New target Dn */
    private Dn newDn;
    

    /**
     * Creates a new instance of MoveOperationContext.
     */
    public MoveOperationContext( CoreSession session )
    {
        super( session );
    }
    

    /**
     * Creates a new instance of MoveOperationContext.
     */
    public MoveOperationContext( CoreSession session, Dn oldDn, Dn newSuperior )
    {
        super( session, oldDn );
        this.newSuperior = newSuperior;
        oldSuperior = oldDn.getParent();
        rdn = (Rdn)(oldDn.getRdn().clone());
        newDn = newSuperior.add( rdn );
    }

    
    public MoveOperationContext( CoreSession session, ModifyDnRequest modifyDnRequest )
    {
        super( session, modifyDnRequest.getName() );
        this.newSuperior = modifyDnRequest.getNewSuperior();
        
        if ( newSuperior == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_326_NEW_SUPERIROR_CANNOT_BE_NULL, modifyDnRequest ) );
        }
        
        this.requestControls = modifyDnRequest.getControls();
        
        if ( modifyDnRequest.getNewRdn() != null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_327_MOVE_AND_RENAME_OPERATION, modifyDnRequest ) );
        }
        
        if ( requestControls.containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            ignoreReferral();
        }
        else
        {
            throwReferral();
        }

        oldSuperior = modifyDnRequest.getName().getParent();
        rdn = (Rdn)(modifyDnRequest.getName().getRdn().clone());
        newDn = newSuperior.add( rdn );
    }


    /**
     *  @return The oldSuperior Dn
     */
    public Dn getOldSuperior()
    {
        return oldSuperior;
    }


    /**
     *  @return The newSuperior Dn
     */
    public Dn getNewSuperior()
    {
        return newSuperior;
    }
    

    /**
     *  @return The Rdn
     */
    public Rdn getRdn()
    {
        return rdn;
    }
    
    
    /**
     *  @return The new Dn
     */
    public Dn getNewDn()
    {
        return newDn;
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
        return "ReplaceContext for old Dn '" + getDn().getName() + "'" +
        ", newSuperior '" + newSuperior + "'";
    }
}
