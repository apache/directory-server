/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.schema;


import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.AbstractSchemaObject;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MutableSchemaObject;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;


/**
 * An ObjectClass implementation used by the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class ObjectClassImpl extends AbstractSchemaObject implements MutableSchemaObject, ObjectClass
{
    private static final long serialVersionUID = 1L;
    private final ObjectClass[] EMPTY_OC_ARRAY = new ObjectClass[0];
    private final String[] EMPTY_STR_ARRAY = new String[0];
    private final AttributeType[] EMPTY_AT_ARRAY = new AttributeType[0];
    
    private final Registries registries;

    private ObjectClassTypeEnum objectClassTypeEnum;
    private ObjectClass[] superClasses;
    private AttributeType[] mayList = EMPTY_AT_ARRAY;
    private AttributeType[] mustList = EMPTY_AT_ARRAY;
    private String[] superClassOids;
    private String[] mayListOids;
    private String[] mustListOids;
    
    
    protected ObjectClassImpl( String oid, Registries registries )
    {
        super( oid );
        this.registries = registries;
    }


    public void setDescription( String description )
    {
        super.setDescription( description );
    }


    public void setNames( String[] names )
    {
        super.setNames( names );
    }


    public void setObsolete( boolean obsolete )
    {
        super.setObsolete( obsolete );
    }

    
    public AttributeType[] getMayList() throws NamingException
    {
        if ( this.mayListOids == null )
        {
            return EMPTY_AT_ARRAY;
        }
        
        for ( int ii = 0; ii < mayListOids.length; ii++ )
        {
            mayList[ii] = registries.getAttributeTypeRegistry().lookup( mayListOids[ii] );
        }
        
        return mayList;
    }
    
    
    public void setMayListOids( String[] mayListOids )
    {
        if ( mayListOids == null )
        {
            this.mayListOids = EMPTY_STR_ARRAY;
            this.mayList = EMPTY_AT_ARRAY;
        }
        else
        {
            this.mayListOids = mayListOids;
            this.mayList = new AttributeType[mayListOids.length];
        }
    }


    public AttributeType[] getMustList() throws NamingException
    {
        if ( this.mustListOids == null )
        {
            return EMPTY_AT_ARRAY;
        }
        
        for ( int ii = 0; ii < mustListOids.length; ii++ )
        {
            mustList[ii] = registries.getAttributeTypeRegistry().lookup( mustListOids[ii] );
        }
        
        return mustList;
    }
    
    
    public void setMustListOids( String[] mustListOids )
    {
        if ( mustListOids == null )
        {
            this.mustListOids = EMPTY_STR_ARRAY;
            this.mustList = EMPTY_AT_ARRAY;
        }
        else
        {
            this.mustListOids = mustListOids;
            this.mustList = new AttributeType[mustListOids.length];
        }
    }


    public ObjectClass[] getSuperClasses() throws NamingException
    {
        if ( superClassOids == null )
        {
            return EMPTY_OC_ARRAY;
        }
        
        for ( int ii = 0; ii < superClassOids.length; ii++ )
        {
            superClasses[ii] = registries.getObjectClassRegistry().lookup( superClassOids[ii] );
        }
        
        return superClasses;
    }

    
    void setSuperClassOids( String[] superClassOids )
    {
        if ( superClassOids == null || superClassOids.length == 0 )
        {
            this.superClassOids = EMPTY_STR_ARRAY;
            this.superClasses = EMPTY_OC_ARRAY;
        }
        else
        {
            this.superClassOids = superClassOids;
            this.superClasses = new ObjectClass[superClassOids.length];
        }
    }
    

    public ObjectClassTypeEnum getType()
    {
        return objectClassTypeEnum;
    }
    
    
    void setType( ObjectClassTypeEnum objectClassTypeEnum )
    {
        this.objectClassTypeEnum = objectClassTypeEnum;
    }
}
