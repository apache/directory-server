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
package org.apache.directory.server.schema.registries;


import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * An AttributeType registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface AttributeTypeRegistry
{
    /**
     * Registers a new AttributeType with this registry.
     *
     * @param schema the name of the schema the AttributeType is associated with
     * @param attributeType the AttributeType to register
     * @throws NamingException if the AttributeType is already registered or
     * the registration operation is not supported
     */
    void register( String schema, AttributeType attributeType ) throws NamingException;


    /**
     * Looks up an AttributeType by its unique Object Identifier or by its
     * unique name.
     * 
     * @param id the object identifier or name of the AttributeType
     * @return the AttributeType instance for the oid
     * @throws NamingException if the AttributeType does not exist
     */
    AttributeType lookup( String id ) throws NamingException;


    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param id the object identifier or the name
     * @return the schema name
     * @throws NamingException if the schema object does not exist
     */
    String getSchemaName( String id ) throws NamingException;


    /**
     * Checks to see if an AttributeType exists.
     * 
     * @param id the object identifier or name of the AttributeType
     * @return true if an AttributeType definition exists for the oid, false
     * otherwise
     */
    boolean hasAttributeType( String id );


    /**
     * Gets an Iterator over the AttributeTypes within this registry.
     *
     * @return an iterator over all AttributeTypes in registry
     */
    Iterator list();
    
    
    /**
     * Gets an oid/name to normalizer mapping used to normalize distinguished 
     * names.
     */
    Map getNormalizerMapping() throws NamingException; 
    
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
    Iterator descendants( String ancestorId ) throws NamingException;

    /**
     * Gets an Iterator over the attributeTypes in this registry.
     * 
     * @return Iterator over attributeTypes
     */
    Iterator<AttributeType> iterator();
}
