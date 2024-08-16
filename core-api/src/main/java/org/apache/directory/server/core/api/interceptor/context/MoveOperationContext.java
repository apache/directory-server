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


import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.i18n.I18n;


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
     * 
     * @param session The session to use
     */
    public MoveOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MOVE ) );
        }
    }


    /**
     * Creates a new instance of MoveOperationContext.
     * 
     * @param session The session to use
     * @param oldDn the original source entry Dn to be moved and renamed
     * @param newSuperior the new entry superior of the target after the move
     */
    public MoveOperationContext( CoreSession session, Dn oldDn, Dn newSuperior )
    {
        super( session, oldDn );
        this.newSuperior = newSuperior;
        oldSuperior = oldDn.getParent();
        rdn = oldDn.getRdn().clone();

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MOVE ) );
        }

        try
        {
            newDn = newSuperior.add( rdn );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new IllegalArgumentException( lide.getMessage(), lide );
        }
    }


    /**
     * Create a new instanc eof MoveOperationContext
     *  
     * @param session The session to use
     * @param modifyDnRequest The ModDN operation to apply
     */
    public MoveOperationContext( CoreSession session, ModifyDnRequest modifyDnRequest )
    {
        super( session, modifyDnRequest.getName() );
        this.newSuperior = modifyDnRequest.getNewSuperior();

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.MOVE ) );
        }

        if ( newSuperior == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_02019_NEW_SUPERIROR_CANNOT_BE_NULL, modifyDnRequest ) );
        }

        this.requestControls = modifyDnRequest.getControls();

        if ( modifyDnRequest.getNewRdn() != null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_02020_MOVE_AND_RENAME_OPERATION, modifyDnRequest ) );
        }

        if ( requestControls.containsKey( ManageDsaIT.OID ) )
        {
            ignoreReferral();
        }
        else
        {
            throwReferral();
        }

        oldSuperior = modifyDnRequest.getName().getParent();
        rdn = modifyDnRequest.getName().getRdn().clone();

        try
        {
            newDn = newSuperior.add( rdn );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new IllegalArgumentException( lide.getMessage(), lide );
        }
    }


    /**
     *  @return The oldSuperior Dn
     */
    public Dn getOldSuperior()
    {
        return oldSuperior;
    }


    /**
     * @param oldSuperior the oldSuperior to set
     */
    public void setOldSuperior( Dn oldSuperior )
    {
        this.oldSuperior = oldSuperior;
    }


    /**
     *  @return The newSuperior Dn
     */
    public Dn getNewSuperior()
    {
        return newSuperior;
    }


    /**
     * @param newSuperior the newSuperior to set
     */
    public void setNewSuperior( Dn newSuperior )
    {
        this.newSuperior = newSuperior;
    }


    /**
     *  @return The Rdn
     */
    public Rdn getRdn()
    {
        return rdn;
    }


    /**
     * @param rdn the rdn to set
     */
    public void setRdn( Rdn rdn )
    {
        this.rdn = rdn;
    }


    /**
     *  @return The new Dn
     */
    public Dn getNewDn()
    {
        return newDn;
    }


    /**
     * @param newDn the newDn to set
     */
    public void setNewDn( Dn newDn )
    {
        this.newDn = newDn;
    }


    /**
     * @return the operation name
     */
    @Override
    public String getName()
    {
        return MessageTypeEnum.MODIFYDN_REQUEST.name();
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return "ReplaceContext for old Dn '" + getDn().getName() + "'" + ", newSuperior '" + newSuperior + "'";
    }
}
