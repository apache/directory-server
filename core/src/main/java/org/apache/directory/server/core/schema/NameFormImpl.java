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
import org.apache.directory.shared.ldap.schema.MutableSchemaObject;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.ObjectClass;


/**
 * A nameForm bean implementation that uses a set of registries to dynamically
 * resolve it's dependencies.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameFormImpl extends AbstractSchemaObject implements NameForm, MutableSchemaObject
{
    private static final long serialVersionUID = 1L;
    private static final String[] EMPTY_STR_ARRAY = new String[0];
    private static final AttributeType[] EMPTY_ATTR_ARRAY = new AttributeType[0];

    
    private final Registries registries;
    
    private String objectClassOid;

    private String[] mayUseOids = EMPTY_STR_ARRAY;
    private AttributeType[] mayUse = EMPTY_ATTR_ARRAY;
    
    private String[] mustUseOids = EMPTY_STR_ARRAY;
    private AttributeType[] mustUse = EMPTY_ATTR_ARRAY;
    

    /**
     * @param oid
     */
    public NameFormImpl( String oid, Registries registries )
    {
        super( oid );
        this.registries = registries;
    }

    
    public void setMayUseOids( String[] mayUseOids )
    {
        if ( mayUseOids == null )
        {
            this.mayUse = EMPTY_ATTR_ARRAY;
            this.mayUseOids = EMPTY_STR_ARRAY;
        }
        else
        {
            this.mayUse = new AttributeType[mayUseOids.length];
            this.mayUseOids = mayUseOids;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.NameForm#getMaytUse()
     */
    public AttributeType[] getMayUse() throws NamingException
    {
        if ( mayUseOids == null || mayUseOids.length == 0 )
        {
            return EMPTY_ATTR_ARRAY;
        }
        
        for ( int ii = 0; ii < mayUseOids.length; ii++ )
        {
            mayUse[ii] = registries.getAttributeTypeRegistry().lookup( mayUseOids[ii] );
        }
        
        return mayUse;
    }


    public void setMustUseOids( String[] mustUseOids )
    {
        if ( mustUseOids == null )
        {
            this.mustUse = EMPTY_ATTR_ARRAY;
            this.mustUseOids = EMPTY_STR_ARRAY;
        }
        else
        {
            this.mustUse = new AttributeType[mustUseOids.length];
            this.mustUseOids = mustUseOids;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.NameForm#getMustUse()
     */
    public AttributeType[] getMustUse() throws NamingException
    {
        if ( mustUseOids == null || mustUseOids.length == 0 )
        {
            return EMPTY_ATTR_ARRAY;
        }
        
        for ( int ii = 0; ii < mustUseOids.length; ii++ )
        {
            mustUse[ii] = registries.getAttributeTypeRegistry().lookup( mustUseOids[ii] );
        }
        
        return mustUse;
    }
    
    
    public void setObjectClassOid( String objectClassOid )
    {
        this.objectClassOid = objectClassOid;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.NameForm#getObjectClass()
     */
    public ObjectClass getObjectClass() throws NamingException
    {
        return registries.getObjectClassRegistry().lookup( objectClassOid );
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
