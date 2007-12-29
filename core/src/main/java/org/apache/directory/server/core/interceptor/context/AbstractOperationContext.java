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


import java.util.HashMap;
import java.util.Map;

import javax.naming.ldap.Control;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * This abstract class stores common context elements, like the DN, which is used
 * in all the contexts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractOperationContext implements OperationContext
{
    private static final Control[] EMPTY_CONTROLS = new Control[0];

    /** The DN associated with the context */
    private LdapDN dn;
    private Map<String, Control> requestControls = new HashMap<String, Control>(4);
    private Map<String, Control> responseControls = new HashMap<String, Control>(4);

    private boolean collateralOperation;
    
    /** The global registries reference */
    private Registries registries;

    
    /**
     * 
     * Creates a new instance of AbstractOperationContext.
     *
     */
    public AbstractOperationContext()
    {
        this.registries = null;
    }


    /**
     * 
     * Creates a new instance of AbstractOperationContext.
     *
     * @param atRegistry The AttributeType registry
     */
    public AbstractOperationContext( Registries registries )
    {
        this.registries = registries;
    }
    
    
    /**
     * Creates a new instance of AbstractOperationContext.
     *
     * @param dn The associated DN
     */
    public AbstractOperationContext( LdapDN dn )
    {
        this.dn = dn;
    }


    /**
     * Creates a new instance of AbstractOperationContext.
     *
     * @param atRegistry The AttributeType registry
     * @param dn The associated DN
     */
    public AbstractOperationContext( Registries registries, LdapDN dn )
    {
        this.dn = dn;
        this.registries = registries;
    }


    /**
     * Creates a new instance of AbstractOperationContext.
     *
     * @param dn the associated DN
     * @param collateralOperation true if op is collateral, false otherwise
     */
    public AbstractOperationContext( LdapDN dn, boolean collateralOperation )
    {
        this.dn = dn;
        this.collateralOperation = collateralOperation;
    }


    /**
     * Creates a new instance of AbstractOperationContext.
     *
     * @param atRegistry The AttributeType registry
     * @param dn the associated DN
     * @param collateralOperation true if op is collateral, false otherwise
     */
    public AbstractOperationContext( Registries registries, LdapDN dn, boolean collateralOperation )
    {
        this.dn = dn;
        this.collateralOperation = collateralOperation;
        this.registries = registries; 
    }


    /**
     * Creates an operation context where the operation is considered a side
     * effect of a direct operation.
     *
     * @param collateralOperation true if this is a side effect operation
     */
    public AbstractOperationContext( boolean collateralOperation )
    {
        this.collateralOperation = collateralOperation;
    }


    /**
     * Creates an operation context where the operation is considered a side
     * effect of a direct operation.
     *
     * @param atRegistry The AttributeType registry
     * @param collateralOperation true if this is a side effect operation
     */
    public AbstractOperationContext( Registries registries, boolean collateralOperation )
    {
        this.collateralOperation = collateralOperation;
        this.registries = registries;
    }


    /**
     * Tells if the current operation is considered a side effect of the
     * current context
     */
    public boolean isCollateralOperation()
    {
        return collateralOperation;
    }


    public void setCollateralOperation( boolean collateralOperation )
    {
        this.collateralOperation = collateralOperation;
    }


    /**
     * @return The associated DN
     */
    public LdapDN getDn()
    {
        return dn;
    }

    
    /**
     * Set the context DN
     *
     * @param dn The DN to set
     */
    public void setDn( LdapDN dn )
    {
        this.dn = dn;
    }

    
    public void addRequestControl( Control requestControl )
    {
        requestControls.put( requestControl.getID(), requestControl );
    }

    
    public Control getRequestControl( String numericOid )
    {
        return requestControls.get( numericOid );
    }

    
    public boolean hasRequestControl( String numericOid )
    {
        return requestControls.containsKey( numericOid );
    }


    public void addResponseControl( Control responseControl )
    {
        responseControls.put( responseControl.getID(), responseControl );
    }


    public Control getResponseControl( String numericOid )
    {
        return responseControls.get( numericOid );
    }


    public boolean hasResponseControl( String numericOid )
    {
        return responseControls.containsKey( numericOid );
    }


    public Control[] getResponseControls()
    {
        if ( responseControls.isEmpty() )
        {
            return EMPTY_CONTROLS;
        }
        
        return responseControls.values().toArray( EMPTY_CONTROLS );
    }


    public boolean hasResponseControls()
    {
        return ! responseControls.isEmpty();
    }


    public int getResponseControlCount()
    {
        return responseControls.size();
    }


    public void addRequestControls( Control[] requestControls )
    {
        for ( Control c : requestControls )
        {
            this.requestControls.put( c.getID(), c );
        }
    }

    
    /**
     * @return The AttributeTypeRegistry
     */
    public Registries getRegistries()
    {
        return registries;
    }
}
