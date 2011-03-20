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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.schema.MutableAttributeTypeImpl;
import org.apache.directory.shared.ldap.model.schema.MutableLdapSyntaxImpl;
import org.apache.directory.shared.ldap.model.schema.MutableMatchingRuleImpl;
import org.apache.directory.shared.ldap.model.schema.AbstractNormalizer;
import org.apache.directory.shared.ldap.model.schema.SchemaObjectType;
import org.apache.directory.shared.ldap.model.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.model.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.model.schema.registries.DefaultSchemaObjectRegistry;
import org.apache.directory.shared.ldap.model.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.model.schema.registries.SchemaObjectRegistry;


/**
 * A mock {@link org.apache.directory.shared.ldap.model.schema.registries.AttributeTypeRegistry} to test {@link ACITupleFilter} implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public class DummyAttributeTypeRegistry extends DefaultSchemaObjectRegistry<MutableAttributeTypeImpl>
{
    private final boolean returnOperational;


    public DummyAttributeTypeRegistry( boolean returnOperational )
    {
        super( SchemaObjectType.ATTRIBUTE_TYPE, new OidRegistry() );
        this.returnOperational = returnOperational;
    }


    public MutableAttributeTypeImpl lookup( final String id ) throws LdapException
    {
        AbstractNormalizer normalizer = new DeepTrimToLowerNormalizer( "1.1.1" );

        MutableMatchingRuleImpl equality = new MutableMatchingRuleImpl( "1.1.1" );
        equality.setNormalizer( normalizer );

        MutableAttributeTypeImpl attributeType = new MutableAttributeTypeImpl( id );
        attributeType.setEquality( equality );
        attributeType.setSingleValued( false );
        attributeType.setCollective( false );
        attributeType.setDescription( id );

        if ( returnOperational )
        {
            attributeType.setUserModifiable( false );
        }
        else
        {
            MutableLdapSyntaxImpl syntax = new MutableLdapSyntaxImpl( "1.1.1" );
            syntax.setHumanReadable( true );

            attributeType.setSyntax( syntax );
            attributeType.setUserModifiable( true );
        }

        return attributeType;
    }


    public String getSchemaName( String id ) throws LdapException
    {
        return "dummy";
    }


    public boolean contains( String id )
    {
        return true;
    }


    public Iterator<MutableAttributeTypeImpl> list()
    {
        return new ArrayList<MutableAttributeTypeImpl>().iterator();
    }


    public Map<String, OidNormalizer> getNormalizerMapping()
    {
        return null;
    }


    public Iterator<MutableAttributeTypeImpl> descendants( String ancestorId ) throws LdapException
    {
        return null;
    }


    public boolean hasDescendants( String ancestorId ) throws LdapException
    {
        return false;
    }


    public Iterator<MutableAttributeTypeImpl> iterator()
    {
        return null;
    }


    public MutableAttributeTypeImpl unregister( String numericOid ) throws LdapException
    {
        return null;
    }


    public void register( MutableAttributeTypeImpl attributeType ) throws LdapException
    {
    }


    public Set<String> getBinaryAttributes() throws LdapException
    {
        return null;
    }


    public void unregisterDescendants( MutableAttributeTypeImpl attributeType, MutableAttributeTypeImpl ancestor ) throws LdapException
    {
    }


    public void registerDescendants( MutableAttributeTypeImpl attributeType, MutableAttributeTypeImpl ancestor ) throws LdapException
    {
    }


    public void addMappingFor( MutableAttributeTypeImpl attributeType ) throws LdapException
    {
    }


    public SchemaObjectRegistry<MutableAttributeTypeImpl> copy()
    {
        return null;
    }
}