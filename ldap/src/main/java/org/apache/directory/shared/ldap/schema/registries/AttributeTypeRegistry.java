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

import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;


/**
 * An AttributeType registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface AttributeTypeRegistry extends SchemaObjectRegistry<AttributeType>, Iterable<AttributeType>
{
    /**
     * Gets an oid/name to normalizer mapping used to normalize distinguished 
     * names.
     *
     * @return a map of OID Strings to OidNormalizer instances
     */
    Map<String, OidNormalizer> getNormalizerMapping();


    /**
     * Quick lookup to see if an attribute has descendants.
     * 
     * @param ancestorId the name alias or OID for an attributeType
     * @return an Iterator over the AttributeTypes which have the ancestor
     * within their superior chain to the top
     * @throws NamingException if the ancestor attributeType cannot be 
     * discerned from the ancestorId supplied
     */
    boolean hasDescendants( String ancestorId ) throws NamingException;


    /**
     * Get's an iterator over the set of descendant attributeTypes for
     * some ancestor's name alias or their OID.
     * 
     * @param ancestorId the name alias or OID for an attributeType
     * @return an Iterator over the AttributeTypes which have the ancestor
     * within their superior chain to the top
     * @throws NamingException if the ancestor attributeType cannot be 
     * discerned from the ancestorId supplied
     */
    Iterator<AttributeType> descendants( String ancestorId ) throws NamingException;


    /**
     * Store the AttributeType into a map associating an AttributeType to its
     * descendants.
     * 
     * @param attributeType The attributeType to register
     * @throws NamingException If something went wrong
     */
    void registerDescendants( AttributeType attributeType, AttributeType ancestor ) throws NamingException;


    /**
     * Remove the AttributeType from the map associating an AttributeType to its
     * descendants.
     * 
     * @param attributeType The attributeType to unregister
     * @param ancestor its ancestor 
     * @throws NamingException If something went wrong
     */
    void unregisterDescendants( AttributeType attributeType, AttributeType ancestor ) throws NamingException;


    /**
     * Add a new Oid/Normalizer couple in the OidNormalizer map
     */
    void addMappingFor( AttributeType attributeType ) throws NamingException;


    /**
     * Remove a new Oid/Normalizer couple in the OidNormalizer map
     */
    void removeMappingFor( AttributeType attributeType ) throws NamingException;


    /**
     * Copy the AttributeTypeRegistry
     */
    AttributeTypeRegistry copy();
}
