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

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * Normalizer registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface NormalizerRegistry
{
    /**
     * Registers a Normalizer with this registry.
     * 
     * @param schema the name of the schema the Normalizer is associated with
     * @param normalizer the Normalizer to register
     * @throws NamingException if the Normalizer is already registered or the
     *      registration operation is not supported
     */
    void register( String schema, String oid, Normalizer normalizer ) throws NamingException;


    /**
     * Looks up a Normalizer by its unique Object Identifier.
     * 
     * @param oid the object identifier
     * @return the Normalizer for the oid
     * @throws NamingException if there is a backing store failure or the 
     *      Normalizer does not exist.
     */
    Normalizer lookup( String oid ) throws NamingException;


    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param oid the object identifier
     * @return the schema name
     * @throws NamingException if the schema object does not exist
     */
    String getSchemaName( String oid ) throws NamingException;


    /**
     * Checks to see if a Normalizer exists.  Backing store failures simply 
     * return false.
     * 
     * @param oid the object identifier
     * @return true if a Normalizer definition exists for the oid, false 
     *      otherwise
     */
    boolean hasNormalizer( String oid );
    
    /**
     * Used to iterate through all normalizers.  We have to iterate over the
     * OID String keys because these objects do not associate a matchingRule OID
     * with them as a class member.
     *  
     * @return an Iterator over the set of OID Strings in this registry
     */
    Iterator<String> oidIterator();

    /**
     * Unregisters a normalizer from this registry by OID.
     * 
     * @param oid the numeric OID of the matchingRule the normalizer is for
     * @throws NamingException if the provided argument is not a numeric OID
     */
    void unregister( String oid ) throws NamingException;
}
