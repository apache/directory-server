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

import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * A DN Name component Normalizer which uses the bootstrap registries to find
 * the appropriate normalizer for the attribute of the name component with which
 * to normalize the name component value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ConcreteNameComponentNormalizer implements NameComponentNormalizer
{
    /** the at registry used to dynamically resolve Normalizers */
    private final AttributeTypeRegistry attributeRegistry;
    /** the oid registry used to dynamically resolve aliases to OIDs */
    private final OidRegistry oidRegistry;


    /**
     * Creates a DN Name component Normalizer which uses the bootstrap
     * registries to find the appropriate normalizer for the attribute of the
     * name component with which to normalize the name component value.
     *
     * @param registry the at registry used to dynamically resolve Normalizers
     */
    public ConcreteNameComponentNormalizer( AttributeTypeRegistry registry, OidRegistry oidRegistry )
    {
        this.attributeRegistry = registry;
        this.oidRegistry = oidRegistry;
    }


    /**
     * @see NameComponentNormalizer#normalizeByName(String, String)
     */
    public Object normalizeByName( String name, String value ) throws NamingException
    {
        return lookup( name ).normalize( value );
    }


    /**
     * @see NameComponentNormalizer#normalizeByName(String, String)
     */
    public Object normalizeByName( String name, byte[] value ) throws NamingException
    {
        return lookup( name ).normalize( value );
    }


    /**
     * @see NameComponentNormalizer#normalizeByOid(String, String)
     */
    public Object normalizeByOid( String oid, String value ) throws NamingException
    {
        return lookup( oid ).normalize( value );
    }


    /**
     * @see NameComponentNormalizer#normalizeByOid(String, String)
     */
    public Object normalizeByOid( String oid, byte[] value ) throws NamingException
    {
        return lookup( oid ).normalize( value );
    }


    /**
     * Looks up the Normalizer to use for a name component using the attributeId
     * for the name component.  First the attribute is resolved, then its
     * equality matching rule is looked up.  The normalizer of that matching
     * rule is returned.
     *
     * @param id the name or oid of the attribute in the name component to
     * normalize the value of
     * @return the Normalizer to use for normalizing the value of the attribute
     * @throws NamingException if there are failures resolving the Normalizer
     */
    private Normalizer lookup( String id ) throws NamingException
    {
        AttributeType type = attributeRegistry.lookup( id );
        MatchingRule mrule = type.getEquality();
        
        if ( mrule == null )
        {
            return NoOpNormalizer.INSTANCE;
        }
        
        return type.getEquality().getNormalizer();
    }


    /**
     * @see NameComponentNormalizer#isDefined(String)
     */
    public boolean isDefined( String id )
    {
        return attributeRegistry.hasAttributeType( id );
    }


    public String normalizeName( String attributeName ) throws NamingException
    {
        return oidRegistry.getOid( attributeName );
    }
}
