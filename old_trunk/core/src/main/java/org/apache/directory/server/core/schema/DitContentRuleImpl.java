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
package org.apache.directory.server.core.schema;


import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.AbstractSchemaObject;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.MutableSchemaObject;
import org.apache.directory.shared.ldap.schema.ObjectClass;


/**
 * A DitContentRule bean implementation that uses a registries object to dynamically
 * resolve it's dependencies.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DitContentRuleImpl extends AbstractSchemaObject implements MutableSchemaObject, DITContentRule
{
    private static final long serialVersionUID = 1L;
    private static final String[] EMPTY_STR_ARRAY = new String[0];
    private static final ObjectClass[] EMPTY_OC_ARRAY = new ObjectClass[0];
    private static final AttributeType[] EMPTY_ATTR_ARRAY = new AttributeType[0];

    private final Registries registries;
    
    private String[] auxObjectClassOids = EMPTY_STR_ARRAY;
    private ObjectClass[] auxObjectClasses = EMPTY_OC_ARRAY;
    
    private String[] mustNameOids = EMPTY_STR_ARRAY;
    private AttributeType[] mustNames = EMPTY_ATTR_ARRAY;
    
    private String[] mayNameOids = EMPTY_STR_ARRAY;
    private AttributeType[] mayNames = EMPTY_ATTR_ARRAY;
    
    private String[] notNameOids = EMPTY_STR_ARRAY;
    private AttributeType[] notNames = EMPTY_ATTR_ARRAY;
    
    
    protected DitContentRuleImpl( String oid, Registries registries )
    {
        super( oid );
        this.registries = registries;
    }

    
    public void setAuxObjectClassOids( String[] auxObjectClassOids )
    {
        if ( auxObjectClassOids == null )
        {
            this.auxObjectClassOids = EMPTY_STR_ARRAY;
            this.auxObjectClasses = EMPTY_OC_ARRAY;
        }
        else
        {
            this.auxObjectClassOids = auxObjectClassOids;
            this.auxObjectClasses = new ObjectClass[auxObjectClassOids.length];
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.DITContentRule#getAuxObjectClasses()
     */
    public ObjectClass[] getAuxObjectClasses() throws NamingException
    {
        if ( auxObjectClassOids == null || auxObjectClassOids.length == 0 )
        {
            return EMPTY_OC_ARRAY;
        }
        
        for ( int ii = 0; ii < auxObjectClassOids.length; ii++ )
        {
            auxObjectClasses[ii] = registries.getObjectClassRegistry().lookup( auxObjectClassOids[ii] );
        }
        
        return auxObjectClasses;
    }

    
    public void setMayNameOids( String[] mayNameOids )
    {
        if ( mayNameOids == null )
        {
            this.mayNameOids = EMPTY_STR_ARRAY;
            this.mayNames = EMPTY_ATTR_ARRAY;
        }
        else
        {
            this.mayNameOids = mayNameOids;
            this.mayNames = new AttributeType[mayNameOids.length];
        }
    }
    

    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.DITContentRule#getMayNames()
     */
    public AttributeType[] getMayNames() throws NamingException
    {
        if ( mayNameOids == null || mayNameOids.length == 0 )
        {
            return EMPTY_ATTR_ARRAY;
        }
        
        for ( int ii = 0; ii < mayNameOids.length; ii++ )
        {
            mayNames[ii] = registries.getAttributeTypeRegistry().lookup( mayNameOids[ii] );
        }
        
        return mayNames;
    }

    
    public void setMustNameOids( String[] mustNameOids )
    {
        if ( mustNameOids == null )
        {
            this.mustNameOids = EMPTY_STR_ARRAY;
            this.mustNames = EMPTY_ATTR_ARRAY;
        }
        else
        {
            this.mustNameOids = mustNameOids;
            this.mustNames = new AttributeType[mustNameOids.length];
        }
    }
    

    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.DITContentRule#getMustNames()
     */
    public AttributeType[] getMustNames() throws NamingException
    {
        if ( mustNameOids == null || mustNameOids.length == 0 )
        {
            return EMPTY_ATTR_ARRAY;
        }
        
        for ( int ii = 0; ii < mustNameOids.length; ii++ )
        {
            mustNames[ii] = registries.getAttributeTypeRegistry().lookup( mustNameOids[ii] );
        }
        
        return mustNames;
    }


    public void setNotNameOids( String[] notNameOids )
    {
        if ( notNameOids == null )
        {
            this.notNameOids = EMPTY_STR_ARRAY;
            this.notNames = EMPTY_ATTR_ARRAY;
        }
        else
        {
            this.notNameOids = notNameOids;
            this.notNames = new AttributeType[notNameOids.length];
        }
    }
    

    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.DITContentRule#getNotNames()
     */
    public AttributeType[] getNotNames() throws NamingException
    {
        if ( notNameOids == null || notNameOids.length == 0 )
        {
            return EMPTY_ATTR_ARRAY;
        }
        
        for ( int ii = 0; ii < notNameOids.length; ii++ )
        {
            notNames[ii] = registries.getAttributeTypeRegistry().lookup( notNameOids[ii] );
        }
        
        return notNames;
    }

    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.DITContentRule#getObjectClass()
     */
    public ObjectClass getObjectClass() throws NamingException
    {
        return registries.getObjectClassRegistry().lookup( getOid() );
    }


    public void setDescription( String description )
    {
        super.setDescription( description );
    }
    
    
    public void setObsolete( boolean obsolete )
    {
        super.setObsolete( obsolete );
    }
    
    
    public void setNames( String[] names )
    {
        super.setNames( names );
    }
    
    
    public void setSchema( String schema )
    {
        super.setSchema( schema );
    }
}
