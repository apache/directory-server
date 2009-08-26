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

import javax.naming.NamingException;

import org.apache.directory.server.core.authz.support.ACITupleFilter;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;


/**
 * A mock {@link AttributeTypeRegistry} to test {@link ACITupleFilter} implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 *
 */
public class DummyAttributeTypeRegistry extends AttributeTypeRegistry
{
    private final boolean returnOperational;


    public DummyAttributeTypeRegistry(boolean returnOperational)
    {
        this.returnOperational = returnOperational;
    }


    public AttributeType lookup( final String id ) throws NamingException
    {
        Normalizer normalizer = new DeepTrimToLowerNormalizer( "1.1.1" );

        MatchingRule equality = new MatchingRule( "1.1.1" );
        equality.setNormalizer( normalizer );
        
        AttributeType attributeType = new AttributeType( id );
        attributeType.setEquality( equality );
        attributeType.setSingleValue( false );
        attributeType.setCollective( false );
        attributeType.setDescription( id );

        if ( returnOperational )
        {
            attributeType.setCanUserModify( false );
        }
        else
        {
            LdapSyntax syntax = new LdapSyntax( "1.1.1" );
            syntax.setHumanReadable( true );

            attributeType.setSyntax( syntax );
            attributeType.setCanUserModify( true );
        }
        
        return attributeType;
    }


    public String getSchemaName( String id ) throws NamingException
    {
        return "dummy";
    }


    public boolean contains( String id )
    {
        return true;
    }


    public Iterator<AttributeType> list()
    {
        return new ArrayList<AttributeType>().iterator();
    }


    public Map<String,OidNormalizer> getNormalizerMapping() throws NamingException
    {
        return null;
    }


    public Iterator<AttributeType> descendants( String ancestorId ) throws NamingException
    {
        return null;
    }


    public boolean hasDescendants( String ancestorId ) throws NamingException
    {
        return false;
    }


    public Iterator<AttributeType> iterator()
    {
        return null;
    }


    public void unregister( String numericOid ) throws NamingException
    {
    }


    public void register( AttributeType attributeType ) throws NamingException
    {
    }


    public Set<String> getBinaryAttributes() throws NamingException
    {
        return null;
    }
}
