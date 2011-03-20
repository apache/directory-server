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
package org.apache.directory.server.core.authz.support;

import java.util.Collection;

import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.MutableAttributeTypeImpl;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;

/**
 * A container used to pass parameters to the ACDF engine
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AciContext
{
    /** The schema manager */
    private SchemaManager schemaManager;
    
    /** The operation context */
    private OperationContext operationContext;
    
    /** The Users belonging to a group */
    private Collection<Dn> userGroupNames;

    /** The user's Dn */
    private Dn userDn;
    
    /** The requested Authentication level (default to NONE) */
    private AuthenticationLevel authenticationLevel = AuthenticationLevel.NONE;
    
    /** the entry's Dn */
    private Dn entryDn;
    
    /** The AttributeType */
    private MutableAttributeTypeImpl attributeType;
    
    /** The attribute's values */
    private Value<?> attrValue;
    
    /** The allowed operations */
    private Collection<MicroOperation> microOperations;
    
    /** The resulting tuples */
    private Collection<ACITuple> aciTuples;
    
    /** The entry */
    private Entry entry;
    
    /** ??? */
    private Entry entryView;
    
    /**
     * Creates a new instance of AciContext.
     *
     * @param schemaManager The SchemaManager instance
     * @param operationContext The OperationContext instance
     */
    public AciContext( SchemaManager schemaManager, OperationContext operationContext )
    {
        this.schemaManager = schemaManager;
        this.operationContext = operationContext;
    }
    
    
    /**
     * @return the schemaManager
     */
    public SchemaManager getSchemaManager()
    {
        return schemaManager;
    }

    /**
     * @param schemaManager the schemaManager to set
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }

    /**
     * @return the operationContext
     */
    public OperationContext getOperationContext()
    {
        return operationContext;
    }

    /**
     * @param operationContext the operationContext to set
     */
    public void setOperationContext( OperationContext operationContext )
    {
        this.operationContext = operationContext;
    }

    /**
     * @return the userGroupNames
     */
    public Collection<Dn> getUserGroupNames()
    {
        return userGroupNames;
    }

    /**
     * @param userGroupNames the userGroupNames to set
     */
    public void setUserGroupNames( Collection<Dn> userGroupNames )
    {
        this.userGroupNames = userGroupNames;
    }

    /**
     * @return the user Dn
     */
    public Dn getUserDn()
    {
        return userDn;
    }

    /**
     * @param userDn the user Dn to set
     */
    public void setUserDn( Dn userDn )
    {
        this.userDn = userDn;
    }

    /**
     * @return the authenticationLevel
     */
    public AuthenticationLevel getAuthenticationLevel()
    {
        return authenticationLevel;
    }

    /**
     * @param authenticationLevel the authenticationLevel to set
     */
    public void setAuthenticationLevel( AuthenticationLevel authenticationLevel )
    {
        this.authenticationLevel = authenticationLevel;
    }

    /**
     * @return the entry Dn
     */
    public Dn getEntryDn()
    {
        return entryDn;
    }

    /**
     * @param entryDn the entry Dn to set
     */
    public void setEntryDn( Dn entryDn )
    {
        this.entryDn = entryDn;
    }

    /**
     * @return the attributeType
     */
    public MutableAttributeTypeImpl getAttributeType()
    {
        return attributeType;
    }

    /**
     * @param attributeType the attributeType to set
     */
    public void setAttributeType( MutableAttributeTypeImpl attributeType )
    {
        this.attributeType = attributeType;
    }

    /**
     * @return the attrValue
     */
    public Value<?> getAttrValue()
    {
        return attrValue;
    }

    /**
     * @param attrValue the attrValue to set
     */
    public void setAttrValue( Value<?> attrValue )
    {
        this.attrValue = attrValue;
    }

    /**
     * @return the microOperations
     */
    public Collection<MicroOperation> getMicroOperations()
    {
        return microOperations;
    }

    /**
     * @param microOperations the microOperations to set
     */
    public void setMicroOperations( Collection<MicroOperation> microOperations )
    {
        this.microOperations = microOperations;
    }

    /**
     * @return the aciTuples
     */
    public Collection<ACITuple> getAciTuples()
    {
        return aciTuples;
    }

    /**
     * @param aciTuples the aciTuples to set
     */
    public void setAciTuples( Collection<ACITuple> aciTuples )
    {
        this.aciTuples = aciTuples;
    }

    /**
     * @return the entry
     */
    public Entry getEntry()
    {
        return entry;
    }

    /**
     * @param entry the entry to set
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }

    /**
     * @return the entryView
     */
    public Entry getEntryView()
    {
        return entryView;
    }

    /**
     * @param entryView the entryView to set
     */
    public void setEntryView( Entry entryView )
    {
        this.entryView = entryView;
    }
}
