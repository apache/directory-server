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
package org.apache.directory.shared.ldap.schema.registries;


import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;


/**
 * An immutable wrapper of the AttributeType registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class ImmutableAttributeTypeRegistry implements AttributeTypeRegistry
{
    /** The wrapped AttributeType registry */
    AttributeTypeRegistry immutableAttributeTypeRegistry;


    /**
     * Creates a new instance of ImmutableAttributeTypeRegistry.
     *
     * @param attributeTypeRegistry The wrapped AttributeType registry
     */
    public ImmutableAttributeTypeRegistry( AttributeTypeRegistry attributeTypeRegistry )
    {
        immutableAttributeTypeRegistry = attributeTypeRegistry;
    }


    /**
     * {@inheritDoc}
     */
    public Map<String, OidNormalizer> getNormalizerMapping()
    {
        return immutableAttributeTypeRegistry.getNormalizerMapping();
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasDescendants( String ancestorId ) throws NamingException
    {
        return immutableAttributeTypeRegistry.hasDescendants( ancestorId );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<AttributeType> descendants( String ancestorId ) throws NamingException
    {
        return immutableAttributeTypeRegistry.descendants( ancestorId );
    }


    /**
     * {@inheritDoc}
     */
    public void register( AttributeType attributeType ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void registerDescendants( AttributeType attributeType, AttributeType ancestor ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterDescendants( AttributeType attributeType, AttributeType ancestor ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public AttributeType unregister( String numericOid ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void addMappingFor( AttributeType attributeType ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void removeMappingFor( AttributeType attributeType ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public AttributeType lookup( String oid ) throws NamingException
    {
        return immutableAttributeTypeRegistry.lookup( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return immutableAttributeTypeRegistry.toString();
    }


    /**
     * {@inheritDoc}
     */
    public AttributeTypeRegistry copy()
    {
        return ( AttributeTypeRegistry ) immutableAttributeTypeRegistry.copy();
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return immutableAttributeTypeRegistry.size();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<AttributeType> iterator()
    {
        return immutableAttributeTypeRegistry.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return immutableAttributeTypeRegistry.oidsIterator();
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        return immutableAttributeTypeRegistry.contains( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String getOidByName( String name ) throws NamingException
    {
        return immutableAttributeTypeRegistry.getOidByName( name );
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws NamingException
    {
        return immutableAttributeTypeRegistry.getSchemaName( oid );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObjectType getType()
    {
        return immutableAttributeTypeRegistry.getType();
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
        // Do nothing
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObject get( String oid )
    {
        return immutableAttributeTypeRegistry.get( oid );
    }


    /**
     * {@inheritDoc}
     */
    public void clear() throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public AttributeType unregister( AttributeType schemaObject ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the AttributeTypeRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }
}
