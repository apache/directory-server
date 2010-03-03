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
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.name.DN;


/**
 * A Move context used for Interceptors. It contains all the informations
 * needed for the modify DN operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MoveOperationContext extends AbstractChangeOperationContext
{
    /** The parent DN */
    private DN parent;
    

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
    public MoveOperationContext( CoreSession session, DN oldDn, DN parent )
    {
        super( session, oldDn );
        this.parent = parent;
    }

    
    public MoveOperationContext( CoreSession session, InternalModifyDnRequest modifyDnRequest )
    {
        super( session, modifyDnRequest.getName() );
        this.parent = modifyDnRequest.getNewSuperior();
        
        if ( parent == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_326, modifyDnRequest ) );
        }
        
        this.requestControls = modifyDnRequest.getControls();
        
        if ( modifyDnRequest.getNewRdn() != null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_327, modifyDnRequest ) );
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
        return "ReplaceContext for old DN '" + getDn().getName() + "'" +
        ", parent '" + parent + "'";
    }
}
